package com.vetpetmon.realmengine.common;

import com.vetpetmon.realmengine.RealmEngine;

import java.util.Calendar;

public class CommonProxy {
    public boolean isClient() {
        return false;
    }
    private RealmEngine.Weekday currentDayOfWeek;

    public RealmEngine.Weekday getCurrentDayOfWeek() {
        return currentDayOfWeek;
    }
    public void setCurrentDayOfWeek() {
        currentDayOfWeek = getWeekday();
    }

    public RealmEngine.Weekday getWeekday() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return switch (dayOfWeek) {
            case Calendar.SUNDAY -> RealmEngine.Weekday.SUNDAY;
            case Calendar.MONDAY -> RealmEngine.Weekday.MONDAY;
            case Calendar.TUESDAY -> RealmEngine.Weekday.TUESDAY;
            case Calendar.WEDNESDAY -> RealmEngine.Weekday.WEDNESDAY;
            case Calendar.THURSDAY -> RealmEngine.Weekday.THURSDAY;
            case Calendar.FRIDAY -> RealmEngine.Weekday.FRIDAY;
            case Calendar.SATURDAY -> RealmEngine.Weekday.SATURDAY;
            default -> RealmEngine.Weekday.SUNDAY; // Fallback
        };
    }
}
