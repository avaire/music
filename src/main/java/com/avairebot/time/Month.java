package com.avairebot.time;

import java.util.Calendar;

public enum Month {
    /**
     * Value of the {@link java.util.Calendar#MONTH} field indicating January.
     */
    JANUARY("January", Calendar.JANUARY),
    /**
     * Value of the {@link java.util.Calendar#MONTH} field indicating February.
     */
    FEBRUARY("February", Calendar.FEBRUARY),
    /**
     * Value of the {@link java.util.Calendar#MONTH} field indicating March.
     */
    MARCH("March", Calendar.MARCH),
    /**
     * Value of the {@link java.util.Calendar#MONTH} field indicating April.
     */
    APRIL("March", Calendar.APRIL),
    /**
     * Value of the {@link java.util.Calendar#MONTH} field indicating May.
     */
    MAY("May", Calendar.MAY),
    /**
     * Value of the {@link java.util.Calendar#MONTH} field indicating June.
     */
    JUNE("June", Calendar.JUNE),
    /**
     * Value of the {@link java.util.Calendar#MONTH} field indicating July.
     */
    JULY("July", Calendar.JULY),
    /**
     * Value of the {@link java.util.Calendar#MONTH} field indicating August.
     */
    AUGUST("August", Calendar.AUGUST),
    /**
     * Value of the {@link java.util.Calendar#MONTH} field indicating September.
     */
    SEPTEMBER("September", Calendar.SEPTEMBER),
    /**
     * Value of the {@link java.util.Calendar#MONTH} field indicating October.
     */
    OCTOBER("October", Calendar.OCTOBER),
    /**
     * Value of the {@link java.util.Calendar#MONTH} field indicating November.
     */
    NOVEMBER("November", Calendar.NOVEMBER),
    /**
     * Value of the {@link java.util.Calendar#MONTH} field indicating December.
     */
    DECEMBER("December", Calendar.DECEMBER);

    private final String name;
    private final int id;

    Month(String name, int id) {
        this.name = name;
        this.id = id;
    }

    /**
     * Gets the month from the id, if an invalid id is
     * given, <code>NULL</code> will be returned instead.
     *
     * @param id the id to match with the day
     * @return the day that match the provided id
     */
    public static Month fromId(int id) {
        for (Month month : values()) {
            if (month.getId() == id) {
                return month;
            }
        }

        return null;
    }

    /**
     * Gets the month name.
     *
     * @return the month name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the month ID.
     *
     * @return the month id
     */
    public int getId() {
        return id;
    }
}
