package com.sentinel.monitoring_service.entity;

public enum SubscriptionTier {
    STARTER("Starter", 3, 300, 0, 0),
    PRO("Professional", 25, 10, 19, 190),
    ENTERPRISE("Enterprise", 1000, 5, 79, 790);

    private final String displayName;
    private final int maxMonitors;
    private final int minIntervalSeconds;
    private final int monthlyPriceUsd;
    private final int annualPriceUsd;

    SubscriptionTier(String displayName, int maxMonitors, int minIntervalSeconds, int monthlyPriceUsd, int annualPriceUsd) {
        this.displayName = displayName;
        this.maxMonitors = maxMonitors;
        this.minIntervalSeconds = minIntervalSeconds;
        this.monthlyPriceUsd = monthlyPriceUsd;
        this.annualPriceUsd = annualPriceUsd;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxMonitors() {
        return maxMonitors;
    }

    public int getMinIntervalSeconds() {
        return minIntervalSeconds;
    }

    public int getMonthlyPriceUsd() {
        return monthlyPriceUsd;
    }

    public int getAnnualPriceUsd() {
        return annualPriceUsd;
    }

    public int getPrice(BillingCycle cycle) {
        return cycle == BillingCycle.ANNUAL ? annualPriceUsd : monthlyPriceUsd;
    }
}
