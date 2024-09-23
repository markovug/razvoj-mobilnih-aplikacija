package hr.tvz.calendarandschedulingapp.entities;

public class Event {
    public String title;
    public boolean isAllDay;
    public String start;
    public String end;
    public String repeat;
    public String location;
    public String notes;
    public String participants;

    public Event() {
    }

    public Event(String title, boolean isAllDay, String start, String end, String repeat, String location, String notes, String participants) {
        this.title = title;
        this.isAllDay = isAllDay;
        this.start = start;
        this.end = end;
        this.repeat = repeat;
        this.location = location;
        this.notes = notes;
        this.participants = participants;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isAllDay() {
        return isAllDay;
    }

    public void setAllDay(boolean allDay) {
        isAllDay = allDay;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getRepeat() {
        return repeat;
    }

    public void setRepeat(String repeat) {
        this.repeat = repeat;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getParticipants() {
        return participants;
    }

    public void setParticipants(String participants) {
        this.participants = participants;
    }
}
