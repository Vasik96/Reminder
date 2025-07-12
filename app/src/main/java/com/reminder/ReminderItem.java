package com.reminder;

import android.icu.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class ReminderItem {
    private String name;
    private String id;
    private int hour;
    private int minute;
    private int year;
    private int month;
    private int day;
    private boolean delivered;
    private ReminderType type;

    public ReminderItem(String name, String id, int hour, int minute, int year, int month, int day, boolean delivered, ReminderType type) {
        this.name = name;
        this.id = (id == null || id.isEmpty()) ? FileHandling.generateUniqueId() : id;
        this.hour = hour;
        this.minute = minute;
        this.year = year;
        this.month = month;
        this.day = day;
        this.delivered = delivered;
        this.type = type;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public enum ReminderType {
        ONE_TIME,
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY,
        MONTHLY_3
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public Categorization.Category getCategory() {
        switch (type) {
            case ONE_TIME:
                return Categorization.Category.CATEGORY_ONETIME;
            case DAILY:
                return Categorization.Category.CATEGORY_DAILY;
            case WEEKLY:
                return Categorization.Category.CATEGORY_WEEKLY;
            case MONTHLY:
                return Categorization.Category.CATEGORY_MONTHLY;
            case MONTHLY_3:
                return Categorization.Category.CATEGORY_QUARTERLY;
            case YEARLY:
                return Categorization.Category.CATEGORY_YEARLY;
            default:
                return Categorization.Category.CATEGORY_ALL;
        }
    }



    public ReminderType getReminderType() {
        return type;
    }

    public String getFormattedTime() {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }



    //dd/mm/yy
    public String getFormattedDate() {
        return String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("id", id);
        jsonObject.put("hour", hour);
        jsonObject.put("minute", minute);
        jsonObject.put("year", year);
        jsonObject.put("month", month);
        jsonObject.put("day", day);
        jsonObject.put("delivered", delivered);
        jsonObject.put("type", type != null ? type.name() : ReminderType.ONE_TIME.name());
        return jsonObject;
    }

    public static ReminderItem fromJSON(JSONObject jsonObject) throws JSONException {
        String name = jsonObject.optString("name", "Untitled");

        String id = jsonObject.optString("id", "");
        if (id.isEmpty()) {
            id = FileHandling.generateUniqueId();
        }

        int hour = jsonObject.optInt("hour", -1);
        int minute = jsonObject.optInt("minute", -1);
        int year = jsonObject.optInt("year", -1);
        int month = jsonObject.optInt("month", -1);
        int day = jsonObject.optInt("day", -1);
        boolean delivered = jsonObject.optBoolean("delivered", false);
        ReminderType type;

        try {
            type = ReminderType.valueOf(jsonObject.optString("type", "ONE_TIME"));
        } catch (IllegalArgumentException e) {
            type = ReminderType.ONE_TIME;
        }

        return new ReminderItem(name, id, hour, minute, year, month, day, delivered, type);
    }



    public void setName(String name) {
        this.name = name;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public void setReminderType(ReminderType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ReminderItem other = (ReminderItem) obj;

        return hour == other.hour &&
                minute == other.minute &&
                year == other.year &&
                month == other.month &&
                day == other.day &&
                delivered == other.delivered &&
                name.equals(other.name) &&
                id.equals(other.id) &&
                type == other.type;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + hour;
        result = 31 * result + minute;
        result = 31 * result + year;
        result = 31 * result + month;
        result = 31 * result + day;
        result = 31 * result + (delivered ? 1 : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

}
