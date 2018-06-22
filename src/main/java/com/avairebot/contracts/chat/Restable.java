package com.avairebot.contracts.chat;

import com.avairebot.utilities.CheckPermissionUtil;
import com.avairebot.utilities.RestActionUtil;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class Restable {

    protected final MessageChannel channel;

    public Restable(MessageChannel channel) {
        this.channel = channel;
    }

    /**
     * Submits a Request for execution.
     * <br>Using the default callback functions:
     * {@link net.dv8tion.jda.core.requests.RestAction#DEFAULT_SUCCESS DEFAULT_SUCCESS} and
     * {@link net.dv8tion.jda.core.requests.RestAction#DEFAULT_FAILURE DEFAULT_FAILURE}
     * <p>
     * <p><b>This method is asynchronous</b>
     */
    public void queue() {
        sendMessage().ifPresent(action -> action.queue(null, RestActionUtil.HANDLE_MESSAGE_CREATE));
    }

    /**
     * Submits a Request for execution.
     * <br>Using the default failure callback function.
     * <p>
     * <p><b>This method is asynchronous</b>
     *
     * @param success The success callback that will be called at a convenient time
     *                for the API. (can be null)
     */
    public void queue(Consumer<Message> success) {
        sendMessage().ifPresent(action -> action.queue(success, RestActionUtil.HANDLE_MESSAGE_CREATE));
    }

    /**
     * Submits a Request for execution.
     * <p>
     * <p><b>This method is asynchronous</b>
     *
     * @param success The success callback that will be called at a convenient time
     *                for the API. (can be null)
     * @param failure The failure callback that will be called if the Request
     *                encounters an exception at its execution point.
     */
    public void queue(Consumer<Message> success, Consumer<Throwable> failure) {
        sendMessage().ifPresent(action -> action.queue(success, failure == null ? RestActionUtil.HANDLE_MESSAGE_CREATE : failure));
    }

    /**
     * Schedules a call to {@link #queue()} to be executed after the specified {@code delay}.
     * <br>This is an <b>asynchronous</b> operation that will return a
     * {@link java.util.concurrent.ScheduledFuture ScheduledFuture} representing the task.
     * <p>
     * <p>This operation gives no access to the response value.
     * <br>Use {@link #queueAfter(long, java.util.concurrent.TimeUnit, java.util.function.Consumer)} to access
     * the success consumer for {@link #queue(java.util.function.Consumer)}!
     * <p>
     * <p>The global JDA {@link java.util.concurrent.ScheduledExecutorService ScheduledExecutorService} is used for this operation.
     * <br>You can change the core pool size for this Executor through {@link net.dv8tion.jda.core.JDABuilder#setCorePoolSize(int) JDABuilder.setCorePoolSize(int)}
     * or provide your own Executor with {@link #queueAfter(long, java.util.concurrent.TimeUnit, java.util.concurrent.ScheduledExecutorService)}
     *
     * @param delay The delay after which this computation should be executed, negative to execute immediately
     * @param unit  The {@link java.util.concurrent.TimeUnit TimeUnit} to convert the specified {@code delay}
     * @return {@link java.util.concurrent.ScheduledFuture ScheduledFuture}
     * representing the delayed operation
     * @throws java.lang.IllegalArgumentException If the provided TimeUnit is {@code null}
     */
    public Future<?> queueAfter(long delay, TimeUnit unit) {
        Optional<MessageAction> messageAction = sendMessage();
        if (messageAction.isPresent()) {
            return messageAction.get().queueAfter(delay, unit);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Schedules a call to {@link #queue(java.util.function.Consumer)} to be executed after the specified {@code delay}.
     * <br>This is an <b>asynchronous</b> operation that will return a
     * {@link java.util.concurrent.ScheduledFuture ScheduledFuture} representing the task.
     * <p>
     * <p>This operation gives no access to the failure callback.
     * <br>Use {@link #queueAfter(long, java.util.concurrent.TimeUnit, java.util.function.Consumer, java.util.function.Consumer)} to access
     * the failure consumer for {@link #queue(java.util.function.Consumer, java.util.function.Consumer)}!
     * <p>
     * <p>The global JDA {@link java.util.concurrent.ScheduledExecutorService ScheduledExecutorService} is used for this operation.
     * <br>You can change the core pool size for this Executor through {@link net.dv8tion.jda.core.JDABuilder#setCorePoolSize(int) JDABuilder.setCorePoolSize(int)}
     * or provide your own Executor with {@link #queueAfter(long, java.util.concurrent.TimeUnit, java.util.function.Consumer, java.util.concurrent.ScheduledExecutorService)}
     *
     * @param delay   The delay after which this computation should be executed, negative to execute immediately
     * @param unit    The {@link java.util.concurrent.TimeUnit TimeUnit} to convert the specified {@code delay}
     * @param success The success {@link java.util.function.Consumer Consumer} that should be called
     *                once the {@link #queue(java.util.function.Consumer)} operation completes successfully.
     * @return {@link java.util.concurrent.ScheduledFuture ScheduledFuture}
     * representing the delayed operation
     * @throws java.lang.IllegalArgumentException If the provided TimeUnit is {@code null}
     */
    public Future<?> queueAfter(long delay, TimeUnit unit, Consumer<Message> success) {
        Optional<MessageAction> messageAction = sendMessage();
        if (messageAction.isPresent()) {
            return messageAction.get().queueAfter(delay, unit, success, RestActionUtil.HANDLE_MESSAGE_CREATE);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Schedules a call to {@link #queue(java.util.function.Consumer, java.util.function.Consumer)}
     * to be executed after the specified {@code delay}.
     * <br>This is an <b>asynchronous</b> operation that will return a
     * {@link java.util.concurrent.ScheduledFuture ScheduledFuture} representing the task.
     * <p>
     * <p>The global JDA {@link java.util.concurrent.ScheduledExecutorService ScheduledExecutorService} is used for this operation.
     * <br>You can change the core pool size for this Executor through {@link net.dv8tion.jda.core.JDABuilder#setCorePoolSize(int) JDABuilder.setCorePoolSize(int)}
     * or provide your own Executor with
     * {@link #queueAfter(long, java.util.concurrent.TimeUnit, java.util.function.Consumer, java.util.function.Consumer, java.util.concurrent.ScheduledExecutorService)}
     *
     * @param delay   The delay after which this computation should be executed, negative to execute immediately
     * @param unit    The {@link java.util.concurrent.TimeUnit TimeUnit} to convert the specified {@code delay}
     * @param success The success {@link java.util.function.Consumer Consumer} that should be called
     *                once the {@link #queue(java.util.function.Consumer, java.util.function.Consumer)} operation completes successfully.
     * @param failure The failure {@link java.util.function.Consumer Consumer} that should be called
     *                in case of an error of the {@link #queue(java.util.function.Consumer, java.util.function.Consumer)} operation.
     * @return {@link java.util.concurrent.ScheduledFuture ScheduledFuture}
     * representing the delayed operation
     * @throws java.lang.IllegalArgumentException If the provided TimeUnit is {@code null}
     */
    public Future<?> queueAfter(long delay, TimeUnit unit, Consumer<Message> success, Consumer<Throwable> failure) {
        Optional<MessageAction> messageAction = sendMessage();
        if (messageAction.isPresent()) {
            return messageAction.get().queueAfter(delay, unit, success, failure == null ? RestActionUtil.HANDLE_MESSAGE_CREATE : failure);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Schedules a call to {@link #queue()} to be executed after the specified {@code delay}.
     * <br>This is an <b>asynchronous</b> operation that will return a
     * {@link java.util.concurrent.ScheduledFuture ScheduledFuture} representing the task.
     * <p>
     * <p>This operation gives no access to the response value.
     * <br>Use {@link #queueAfter(long, java.util.concurrent.TimeUnit, java.util.function.Consumer)} to access
     * the success consumer for {@link #queue(java.util.function.Consumer)}!
     * <p>
     * <p>The specified {@link java.util.concurrent.ScheduledExecutorService ScheduledExecutorService} is used for this operation.
     *
     * @param delay    The delay after which this computation should be executed, negative to execute immediately
     * @param unit     The {@link java.util.concurrent.TimeUnit TimeUnit} to convert the specified {@code delay}
     * @param executor The Non-null {@link java.util.concurrent.ScheduledExecutorService ScheduledExecutorService} that should be used
     *                 to schedule this operation
     * @return {@link java.util.concurrent.ScheduledFuture ScheduledFuture}
     * representing the delayed operation
     * @throws java.lang.IllegalArgumentException If the provided TimeUnit or ScheduledExecutorService is {@code null}
     */
    public Future<?> queueAfter(long delay, TimeUnit unit, ScheduledExecutorService executor) {
        Optional<MessageAction> messageAction = sendMessage();
        if (messageAction.isPresent()) {
            return messageAction.get().queueAfter(delay, unit, null, RestActionUtil.HANDLE_MESSAGE_CREATE, executor);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Schedules a call to {@link #queue(java.util.function.Consumer)} to be executed after the specified {@code delay}.
     * <br>This is an <b>asynchronous</b> operation that will return a
     * {@link java.util.concurrent.ScheduledFuture ScheduledFuture} representing the task.
     * <p>
     * <p>This operation gives no access to the failure callback.
     * <br>Use {@link #queueAfter(long, java.util.concurrent.TimeUnit, java.util.function.Consumer, java.util.function.Consumer)} to access
     * the failure consumer for {@link #queue(java.util.function.Consumer, java.util.function.Consumer)}!
     * <p>
     * <p>The specified {@link java.util.concurrent.ScheduledExecutorService ScheduledExecutorService} is used for this operation.
     *
     * @param delay    The delay after which this computation should be executed, negative to execute immediately
     * @param unit     The {@link java.util.concurrent.TimeUnit TimeUnit} to convert the specified {@code delay}
     * @param success  The success {@link java.util.function.Consumer Consumer} that should be called
     *                 once the {@link #queue(java.util.function.Consumer)} operation completes successfully.
     * @param executor The Non-null {@link java.util.concurrent.ScheduledExecutorService ScheduledExecutorService} that should be used
     *                 to schedule this operation
     * @return {@link java.util.concurrent.ScheduledFuture ScheduledFuture}
     * representing the delayed operation
     * @throws java.lang.IllegalArgumentException If the provided TimeUnit or ScheduledExecutorService is {@code null}
     */
    public Future<?> queueAfter(long delay, TimeUnit unit, Consumer<Message> success, ScheduledExecutorService executor) {
        Optional<MessageAction> messageAction = sendMessage();
        if (messageAction.isPresent()) {
            return messageAction.get().queueAfter(delay, unit, success, RestActionUtil.HANDLE_MESSAGE_CREATE, executor);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Schedules a call to {@link #queue(java.util.function.Consumer, java.util.function.Consumer)}
     * to be executed after the specified {@code delay}.
     * <br>This is an <b>asynchronous</b> operation that will return a
     * {@link java.util.concurrent.ScheduledFuture ScheduledFuture} representing the task.
     * <p>
     * <p>The specified {@link java.util.concurrent.ScheduledExecutorService ScheduledExecutorService} is used for this operation.
     *
     * @param delay    The delay after which this computation should be executed, negative to execute immediately
     * @param unit     The {@link TimeUnit TimeUnit} to convert the specified {@code delay}
     * @param success  The success {@link Consumer Consumer} that should be called
     *                 once the {@link #queue(Consumer, Consumer)} operation completes successfully.
     * @param failure  The failure {@link Consumer Consumer} that should be called
     *                 in case of an error of the {@link #queue(Consumer, Consumer)} operation.
     * @param executor The Non-null {@link ScheduledExecutorService ScheduledExecutorService} that should be used
     *                 to schedule this operation
     * @return {@link java.util.concurrent.ScheduledFuture ScheduledFuture}
     * representing the delayed operation
     * @throws java.lang.IllegalArgumentException If the provided TimeUnit or ScheduledExecutorService is {@code null}
     */
    public Future<?> queueAfter(long delay, TimeUnit unit, Consumer<Message> success, Consumer<Throwable> failure, ScheduledExecutorService executor) {
        Optional<MessageAction> messageAction = sendMessage();
        if (messageAction.isPresent()) {
            if (failure == null) {
                failure = RestActionUtil.HANDLE_MESSAGE_CREATE;
            }
            return messageAction.get().queueAfter(delay, unit, success, failure, executor);
        }
        return CompletableFuture.completedFuture(null);
    }

    private Optional<MessageAction> sendMessage() {
        if (channel == null) {
            throw new RuntimeException("Invalid channel given, the channel can not be null!");
        }

        CheckPermissionUtil.PermissionCheckType type = CheckPermissionUtil.canSendMessages(channel);
        if (type.canSendEmbed()) {
            return Optional.of(channel.sendMessage(buildEmbed()));
        }

        if (type.canSendMessage()) {
            String message = toString();
            if (message == null || message.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(channel.sendMessage(toString()));
        }

        return Optional.empty();
    }

    public abstract MessageEmbed buildEmbed();
}
