package io.clamped.server.model;

import java.util.List;
import java.util.Map;

public class StatsResponse {
    public Map<String, Integer> countBySeverity;
    public Map<String, Integer> countByStatus;
    public List<TagCount> topTags;
    public List<TagCount> topExceptionClasses;
    public List<TagCount> topSourceLocations;
    public List<TimelinePoint> timeline;

    public static class TimelinePoint {
        public String hour;
        public int count;
        public TimelinePoint(String hour, int count) {
            this.hour = hour;
            this.count = count;
        }
    }

    public static class TagCount {
        public String tag;
        public int count;

        public TagCount(String tag, int count) {
            this.tag = tag;
            this.count = count;
        }
    }
}
