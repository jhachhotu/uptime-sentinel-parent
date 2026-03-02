package com.sentinel.monitoring_service.dto;

public class WebsiteStatsDTO {
    private String url;
    private String name;
    private String status;
    private double uptimePercentage;
    private long avgResponseTime;
    private int incidentCount;
    private String totalDowntime;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getUptimePercentage() {
        return uptimePercentage;
    }

    public void setUptimePercentage(double uptimePercentage) {
        this.uptimePercentage = uptimePercentage;
    }

    public long getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(long avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public int getIncidentCount() {
        return incidentCount;
    }

    public void setIncidentCount(int incidentCount) {
        this.incidentCount = incidentCount;
    }

    public String getTotalDowntime() {
        return totalDowntime;
    }

    public void setTotalDowntime(String totalDowntime) {
        this.totalDowntime = totalDowntime;
    }
}
