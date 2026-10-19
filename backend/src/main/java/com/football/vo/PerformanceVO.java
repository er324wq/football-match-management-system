package com.football.vo;

public class PerformanceVO {
    private String health;
    private String cpuUsage;
    private String memoryUsed;
    private String httpRequests;
    private String dbStatus;

    public String getHealth() {
        return health;
    }

    public void setHealth(String health) {
        this.health = health;
    }

    public String getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(String cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public String getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(String memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public String getHttpRequests() {
        return httpRequests;
    }

    public void setHttpRequests(String httpRequests) {
        this.httpRequests = httpRequests;
    }

    public String getDbStatus() {
        return dbStatus;
    }

    public void setDbStatus(String dbStatus) {
        this.dbStatus = dbStatus;
    }
}
