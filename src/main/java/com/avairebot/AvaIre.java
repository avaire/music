package com.avairebot;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import com.avairebot.audio.AudioHandler;
import com.avairebot.audio.GuildMusicManager;
import com.avairebot.audio.LavalinkManager;
import com.avairebot.cache.CacheManager;
import com.avairebot.commands.CategoryHandler;
import com.avairebot.commands.CommandHandler;
import com.avairebot.config.Configuration;
import com.avairebot.contracts.commands.Command;
import com.avairebot.contracts.reflection.Reflectional;
import com.avairebot.contracts.scheduler.Job;
import com.avairebot.database.DatabaseManager;
import com.avairebot.database.migrate.migrations.CreateGuildTableMigration;
import com.avairebot.database.migrate.migrations.CreateGuildTypeTableMigration;
import com.avairebot.database.migrate.migrations.CreateMusicPlaylistsTableMigration;
import com.avairebot.database.serializer.PlaylistSongSerializer;
import com.avairebot.database.transformers.PlaylistTransformer;
import com.avairebot.exceptions.InvalidApplicationEnvironmentException;
import com.avairebot.handlers.MainEventHandler;
import com.avairebot.language.I18n;
import com.avairebot.middleware.*;
import com.avairebot.scheduler.ScheduleHandler;
import com.avairebot.shard.ShardEntityCounter;
import com.avairebot.shared.DiscordConstants;
import com.avairebot.shared.ExitCodes;
import com.avairebot.shared.SentryConstants;
import com.avairebot.time.Carbon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import io.sentry.Sentry;
import io.sentry.SentryClient;
import io.sentry.logback.SentryAppender;
import lavalink.client.io.Link;
import lavalink.client.player.LavalinkPlayer;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.utils.SessionControllerAdapter;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

public class AvaIre {

    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(
            PlaylistTransformer.PlaylistSong.class,
            new PlaylistSongSerializer()
        )
        .disableHtmlEscaping()
        .serializeNulls()
        .create();

    private static final Logger LOGGER = LoggerFactory.getLogger(AvaIre.class);
    private static Environment APPLICATION_ENVIRONMENT;
    private final Settings settings;
    private final Configuration config;
    private final CacheManager cache;
    private final DatabaseManager database;
    private final ShardEntityCounter shardEntityCounter;

    private Carbon shutdownTime = null;
    private int shutdownCode = ExitCodes.EXIT_CODE_RESTART;

    private ShardManager shardManager = null;

    public AvaIre(Settings settings) throws IOException, SQLException, InvalidApplicationEnvironmentException {
        this.settings = settings;

        System.out.println(getVersionInfo());

        LOGGER.debug("====================================================");
        LOGGER.debug("Starting the application with debug logging enabled!");
        LOGGER.debug("====================================================\n");

        LOGGER.info("Bootstrapping AvaIre Music v" + AppInfo.getAppInfo().VERSION);
        Reflections.log = null;

        this.cache = new CacheManager(this);

        LOGGER.info("Loading configuration");
        config = new Configuration(this, null, "config.yml");
        if (!config.exists()) {
            getLogger().info("The {} configuration file is missing!", "config.yml");
            getLogger().info("Creating file and terminating program...");

            config.saveDefaultConfig();

            System.exit(ExitCodes.EXIT_CODE_NORMAL);
        }

        APPLICATION_ENVIRONMENT = Environment.fromName(config.getString("environment", "production"));
        if (APPLICATION_ENVIRONMENT == null) {
            throw new InvalidApplicationEnvironmentException(config.getString("environment", "production"));
        }
        LOGGER.info("Starting application in \"{}\" mode", APPLICATION_ENVIRONMENT.getName());
        if (APPLICATION_ENVIRONMENT.equals(Environment.DEVELOPMENT)) {
            RestAction.setPassContext(true);
            RestAction.DEFAULT_FAILURE = Throwable::printStackTrace;
            LOGGER.info("Enabling rest action context parsing and printing stack traces for optimal debugging");
        }

        LOGGER.info("Registering and connecting to database");
        database = new DatabaseManager(this);

        LOGGER.info("Registering database table migrations");
        database.getMigrations().register(
            new CreateGuildTableMigration(),
            new CreateGuildTypeTableMigration(),
            new CreateMusicPlaylistsTableMigration()
        );

        LOGGER.info("Registering default middlewares");
        MiddlewareHandler.initialize(this);
        MiddlewareHandler.register("isBotAdmin", new IsBotAdminMiddleware(this));
        MiddlewareHandler.register("require", new RequirePermissionMiddleware(this));
        MiddlewareHandler.register("hasDJLevel", new RequireDJLevelMiddleware(this));
        MiddlewareHandler.register("throttle", new ThrottleMiddleware(this));
        MiddlewareHandler.register("musicChannel", new IsMusicChannelMiddleware(this));

        LOGGER.info("Registering default command categories");
        String defaultPrefix = getConfig().getString("default-prefix", DiscordConstants.DEFAULT_COMMAND_PREFIX);
        CategoryHandler.setPrefix(defaultPrefix);
        CategoryHandler.addCategory("Help");
        CategoryHandler.addCategory("Music");
        CategoryHandler.addCategory("System");

        LOGGER.info("Registering commands...");
        autoloadPackage(Constants.PACKAGE_COMMAND_PATH, command -> CommandHandler.register((Command) command));
        LOGGER.info(String.format("\tRegistered %s commands successfully!", CommandHandler.getCommands().size()));

        LOGGER.info("Registering jobs...");
        autoloadPackage(Constants.PACKAGE_JOB_PATH, job -> ScheduleHandler.registerJob((Job) job));
        LOGGER.info(String.format("\tRegistered %s jobs successfully!", ScheduleHandler.entrySet().size()));

        LOGGER.info("Preparing I18n");
        I18n.start(this);

        LOGGER.info("Running database migrations");
        database.getMigrations().up();

        String sentryDsn = config.getString("sentryDsn", "").trim();
        if (sentryDsn.length() > 0) {
            LOGGER.info("SentryDSN found, initializing Sentry.io");
            SentryClient sentryClient = Sentry.init(sentryDsn);

            sentryClient.addMdcTag(SentryConstants.SENTRY_MDC_TAG_GUILD);
            sentryClient.addMdcTag(SentryConstants.SENTRY_MDC_TAG_CHANNEL);
            sentryClient.addMdcTag(SentryConstants.SENTRY_MDC_TAG_SHARD);
            sentryClient.addMdcTag(SentryConstants.SENTRY_MDC_TAG_AUTHOR);
            sentryClient.addMdcTag(SentryConstants.SENTRY_MDC_TAG_MESSAGE);

            sentryClient.setEnvironment(getEnvironment().getName());
            switch (getEnvironment()) {
                case PRODUCTION:
                    sentryClient.setRelease(GitInfo.getGitInfo().commitId);
                    break;

                default:
                    sentryClient.setRelease(AppInfo.getAppInfo().VERSION);
                    break;
            }

            getSentryLogbackAppender().start();
        } else {
            getSentryLogbackAppender().stop();
        }

        LOGGER.info("Preparing Lavalink");
        AudioHandler.setAvaire(this);
        LavalinkManager.LavalinkManagerHolder.LAVALINK.start(this);

        try {
            AudioConfiguration.ResamplingQuality.valueOf(
                getConfig().getString("audio-quality.resampling", "medium").toUpperCase()
            );
        } catch (IllegalArgumentException ignored) {
            LOGGER.warn("Invalid audio resampling quality given, \"{}\" is not a valid quality name, using medium quality instead.",
                getConfig().getString("audio-quality.resampling", "medium")
            );
            config.set("audio-quality.resampling", "medium");
        }

        LOGGER.info("Creating bot instance and connecting to Discord network");

        shardEntityCounter = new ShardEntityCounter(this);

        try {
            shardManager = buildShardManager();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static Environment getEnvironment() {
        return APPLICATION_ENVIRONMENT;
    }

    static String getVersionInfo() {
        return "\n\n" +
            "     ___   ____    ____  ___       __  .______       _______ \n" +
            "    /   \\  \\   \\  /   / /   \\     |  | |   _  \\     |   ____|\n" +
            "   /  ^  \\  \\   \\/   / /  ^  \\    |  | |  |_)  |    |  |__   \n" +
            "  /  /_\\  \\  \\      / /  /_\\  \\   |  | |      /     |   __|    Music\n" +
            " /  _____  \\  \\    / /  _____  \\  |  | |  |\\  \\----.|  |____ \n" +
            "/__/     \\__\\  \\__/ /__/     \\__\\ |__| | _| `._____||_______|\n" +
            ""
            + "\n\tVersion:       " + AppInfo.getAppInfo().VERSION
            + "\n\tJVM:           " + System.getProperty("java.version")
            + "\n\tJDA:           " + JDAInfo.VERSION
            + "\n\tLavaplayer     " + PlayerLibrary.VERSION
            + "\n";
    }

    /**
     * Checks if we're ready yet by checking if all the shards are connected and ready to serve events.
     *
     * @return <code>True</code> if all shards has connected and are ready, <code>False</code> otherwise.
     */
    public boolean areWeReadyYet() {
        if (getShardManager() == null) {
            return false;
        }

        for (JDA shard : getShardManager().getShards()) {
            if (shard.getStatus() != JDA.Status.CONNECTED) {
                return false;
            }
        }
        return true;
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    public ShardEntityCounter getShardEntityCounter() {
        return shardEntityCounter;
    }

    public SelfUser getSelfUser() {
        return getShardManager().getShards().get(0).getSelfUser();
    }

    public Settings getSettings() {
        return settings;
    }

    public Configuration getConfig() {
        return config;
    }

    public CacheManager getCache() {
        return cache;
    }

    public DatabaseManager getDatabase() {
        return database;
    }

    public void shutdown() {
        shutdown(ExitCodes.EXIT_CODE_RESTART);
    }

    public void shutdown(int exitCode) {
        getLogger().info("Shutting down bot instance gracefully with exit code " + exitCode);

        for (GuildMusicManager manager : AudioHandler.getDefaultAudioHandler().musicManagers.values()) {
            if (manager.getLastActiveMessage() != null) {
                manager.getLastActiveMessage().makeInfo(
                    "Bot is restarting, sorry for the inconvenience, we'll be right back!"
                ).queue();
            }

            manager.getScheduler().getQueue().clear();

            if (manager.getLastActiveMessage() != null) {
                LavalinkManager.LavalinkManagerHolder.LAVALINK.closeConnection(manager.getLastActiveMessage().getGuild());
            }

            if (manager.getPlayer() instanceof LavalinkPlayer) {
                LavalinkPlayer player = (LavalinkPlayer) manager.getPlayer();

                Link.State state = player.getLink().getState();

                if (player.getLink() != null && !state.equals(Link.State.DESTROYED) && !state.equals(Link.State.DESTROYING)) {
                    player.getLink().destroy();
                }
            }
        }

        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (JDA shard : getShardManager().getShards()) {
            shard.shutdown();
        }

        for (ScheduledFuture<?> job : ScheduleHandler.entrySet()) {
            job.cancel(true);
        }

        try {
            getDatabase().getConnection().close();
        } catch (SQLException ex) {
            getLogger().error("Failed to close database connection during shutdown: ", ex);
        }

        System.exit(exitCode);
    }

    public void scheduleShutdown(Carbon time, int exitCode) {
        shutdownTime = time;
        shutdownCode = exitCode;
    }

    public Carbon getShutdownTime() {
        return shutdownTime;
    }

    public int getShutdownCode() {
        return shutdownCode;
    }

    private ShardManager buildShardManager() throws LoginException {
        DefaultShardManagerBuilder builder = new DefaultShardManagerBuilder()
            .setSessionController(new SessionControllerAdapter())
            .setToken(getConfig().getString("discord.token"))
            .setGame(Game.watching("my code start up..."))
            .setBulkDeleteSplittingEnabled(false)
            .setEnableShutdownHook(false)
            .setAutoReconnect(true)
            .setAudioEnabled(true)
            .setContextEnabled(true)
            .setShardsTotal(settings.getShardCount());

        builder.addEventListeners(new MainEventHandler(this));

        if (LavalinkManager.LavalinkManagerHolder.LAVALINK.isEnabled()) {
            builder.addEventListeners(LavalinkManager.LavalinkManagerHolder.LAVALINK.getLavalink());
        }

        return builder.build();
    }

    private synchronized SentryAppender getSentryLogbackAppender() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

        SentryAppender sentryAppender = (SentryAppender) root.getAppender(SentryConstants.SENTRY_APPENDER_NAME);
        if (sentryAppender == null) {
            sentryAppender = new SentryAppender();
            sentryAppender.setName(SentryConstants.SENTRY_APPENDER_NAME);

            ThresholdFilter warningsOrAboveFilter = new ThresholdFilter();
            warningsOrAboveFilter.setLevel(Level.WARN.levelStr);
            warningsOrAboveFilter.start();
            sentryAppender.addFilter(warningsOrAboveFilter);

            sentryAppender.setContext(loggerContext);
            root.addAppender(sentryAppender);
        }
        return sentryAppender;
    }

    private void autoloadPackage(String path, Consumer<Reflectional> callback) {
        Set<Class<? extends Reflectional>> types = new Reflections(path).getSubTypesOf(Reflectional.class);

        Class[] arguments = new Class[1];
        arguments[0] = AvaIre.class;

        for (Class<? extends Reflectional> reflectionClass : types) {
            if (reflectionClass.getPackage().getName().contains("contracts")) {
                continue;
            }

            try {
                //noinspection JavaReflectionMemberAccess
                callback.accept(reflectionClass.getDeclaredConstructor(arguments).newInstance(this));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                getLogger().error("Failed to create a new instance of package {}", reflectionClass.getName(), e);
            }
        }
    }
}
