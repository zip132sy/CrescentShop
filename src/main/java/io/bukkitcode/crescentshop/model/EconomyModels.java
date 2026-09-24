package io.bukkitcode.crescentshop.model;

import java.util.UUID;

/** 经济数据模型：转账、借款、欠款。 */
public final class EconomyModels {

    private EconomyModels() {
    }

    public static class Transfer {
        private final UUID sender;
        private final UUID receiver;
        private final double amount;
        private final String note;
        private final long createTime;
        private final long expireTime;

        public Transfer(UUID sender, UUID receiver, double amount, String note,
                        long createTime, long expireTime) {
            this.sender = sender;
            this.receiver = receiver;
            this.amount = amount;
            this.note = note;
            this.createTime = createTime;
            this.expireTime = expireTime;
        }

        public UUID getSender() {
            return sender;
        }

        public UUID getReceiver() {
            return receiver;
        }

        public double getAmount() {
            return amount;
        }

        public String getNote() {
            return note;
        }

        public long getCreateTime() {
            return createTime;
        }

        public long getExpireTime() {
            return expireTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    public static class Loan {
        public enum Status {PENDING, ACTIVE, REPAID, SETTLED}

        private final UUID borrower;
        private final UUID lender;
        private final double amount;
        private final String note;
        private double interestRate;
        private int days;
        private Status status;
        private final long createTime;
        private long dueTime;
        private double repaidAmount;

        public Loan(UUID borrower, UUID lender, double amount, String note, long createTime) {
            this.borrower = borrower;
            this.lender = lender;
            this.amount = amount;
            this.note = note;
            this.createTime = createTime;
            this.status = Status.PENDING;
        }

        public UUID getBorrower() {
            return borrower;
        }

        public UUID getLender() {
            return lender;
        }

        public double getAmount() {
            return amount;
        }

        public String getNote() {
            return note;
        }

        public double getInterestRate() {
            return interestRate;
        }

        public void setInterestRate(double interestRate) {
            this.interestRate = interestRate;
        }

        public int getDays() {
            return days;
        }

        public void setDays(int days) {
            this.days = days;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public long getCreateTime() {
            return createTime;
        }

        public long getDueTime() {
            return dueTime;
        }

        public void setDueTime(long dueTime) {
            this.dueTime = dueTime;
        }

        public double getRepaidAmount() {
            return repaidAmount;
        }

        public void setRepaidAmount(double repaidAmount) {
            this.repaidAmount = repaidAmount;
        }

        public double getTotalDue() {
            return amount * (1.0 + interestRate);
        }

        public double getRemaining() {
            return Math.max(0, getTotalDue() - repaidAmount);
        }

        public boolean isDue() {
            return status == Status.ACTIVE && System.currentTimeMillis() > dueTime;
        }

        public boolean isNearDue() {
            if (status != Status.ACTIVE) {
                return false;
            }
            long remaining = dueTime - System.currentTimeMillis();
            return remaining > 0 && remaining <= 24L * 60 * 60 * 1000;
        }
    }

    public static class Debt {
        private final UUID borrower;
        private final UUID lender;
        private final double principal;
        private final double interestRate;
        private final long dueTime;
        private long lastPenaltyTime;

        public Debt(UUID borrower, UUID lender, double principal, double interestRate,
                    long dueTime, long lastPenaltyTime) {
            this.borrower = borrower;
            this.lender = lender;
            this.principal = principal;
            this.interestRate = interestRate;
            this.dueTime = dueTime;
            this.lastPenaltyTime = lastPenaltyTime;
        }

        public UUID getBorrower() {
            return borrower;
        }

        public UUID getLender() {
            return lender;
        }

        public double getPrincipal() {
            return principal;
        }

        public double getInterestRate() {
            return interestRate;
        }

        public long getDueTime() {
            return dueTime;
        }

        public long getLastPenaltyTime() {
            return lastPenaltyTime;
        }

        public void setLastPenaltyTime(long lastPenaltyTime) {
            this.lastPenaltyTime = lastPenaltyTime;
        }

        public double getTotalOwed(double penaltyPerDay) {
            double base = principal * (1.0 + interestRate);
            long overdueMillis = System.currentTimeMillis() - dueTime;
            if (overdueMillis <= 0) {
                return base;
            }
            long overdueDays = overdueMillis / (24L * 60 * 60 * 1000);
            return base + overdueDays * penaltyPerDay;
        }
    }
}
