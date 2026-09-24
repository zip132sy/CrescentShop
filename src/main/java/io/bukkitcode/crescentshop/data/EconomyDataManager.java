package io.bukkitcode.crescentshop.data;

import io.bukkitcode.crescentshop.CrescentShop;
import io.bukkitcode.crescentshop.model.EconomyModels.Debt;
import io.bukkitcode.crescentshop.model.EconomyModels.Loan;
import io.bukkitcode.crescentshop.model.EconomyModels.Transfer;
import io.bukkitcode.crescentshop.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 经济数据管理器：转账、借款、欠款、收款记录。 */
public class EconomyDataManager {

    /** 一条收款记录 */
    public static class Income {
        private final long time;
        private final String source;
        private final double amount;
        private final String from;

        public Income(long time, String source, double amount, String from) {
            this.time = time;
            this.source = source;
            this.amount = amount;
            this.from = from;
        }

        public long getTime() {
            return time;
        }

        public String getSource() {
            return source;
        }

        public double getAmount() {
            return amount;
        }

        public String getFrom() {
            return from;
        }
    }

    private final CrescentShop plugin;
    private final File file;
    private final List<Transfer> transfers = new ArrayList<>();
    private final List<Loan> loans = new ArrayList<>();
    private final List<Debt> debts = new ArrayList<>();
    private final Map<UUID, List<Income>> incomes = new HashMap<>();

    public EconomyDataManager(CrescentShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/economy.yml");
    }

    public void load() {
        transfers.clear();
        loans.clear();
        debts.clear();
        incomes.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection tRoot = config.getConfigurationSection("transfers");
        if (tRoot != null) {
            for (String key : tRoot.getKeys(false)) {
                ConfigurationSection sec = tRoot.getConfigurationSection(key);
                if (sec == null) {
                    continue;
                }
                try {
                    transfers.add(new Transfer(
                            UUID.fromString(sec.getString("sender", "")),
                            UUID.fromString(sec.getString("receiver", "")),
                            sec.getDouble("amount", 0), sec.getString("note", ""),
                            sec.getLong("create-time", System.currentTimeMillis()),
                            sec.getLong("expire-time", System.currentTimeMillis())));
                } catch (Exception ignored) {
                }
            }
        }
        ConfigurationSection lRoot = config.getConfigurationSection("loans");
        if (lRoot != null) {
            for (String key : lRoot.getKeys(false)) {
                ConfigurationSection sec = lRoot.getConfigurationSection(key);
                if (sec == null) {
                    continue;
                }
                try {
                    Loan loan = new Loan(
                            UUID.fromString(sec.getString("borrower", "")),
                            UUID.fromString(sec.getString("lender", "")),
                            sec.getDouble("amount", 0), sec.getString("note", ""),
                            sec.getLong("create-time", System.currentTimeMillis()));
                    loan.setInterestRate(sec.getDouble("interest-rate", 0));
                    loan.setDays(sec.getInt("days", 0));
                    loan.setDueTime(sec.getLong("due-time", 0));
                    loan.setRepaidAmount(sec.getDouble("repaid", 0));
                    try {
                        loan.setStatus(Loan.Status.valueOf(sec.getString("status", "PENDING")));
                    } catch (IllegalArgumentException ignored) {
                        loan.setStatus(Loan.Status.PENDING);
                    }
                    loans.add(loan);
                } catch (Exception ignored) {
                }
            }
        }
        ConfigurationSection dRoot = config.getConfigurationSection("debts");
        if (dRoot != null) {
            for (String key : dRoot.getKeys(false)) {
                ConfigurationSection sec = dRoot.getConfigurationSection(key);
                if (sec == null) {
                    continue;
                }
                try {
                    debts.add(new Debt(
                            UUID.fromString(sec.getString("borrower", "")),
                            UUID.fromString(sec.getString("lender", "")),
                            sec.getDouble("principal", 0),
                            sec.getDouble("interest-rate", 0),
                            sec.getLong("due-time", System.currentTimeMillis()),
                            sec.getLong("last-penalty-time", System.currentTimeMillis())));
                } catch (Exception ignored) {
                }
            }
        }
        ConfigurationSection iRoot = config.getConfigurationSection("incomes");
        if (iRoot != null) {
            for (String uuidKey : iRoot.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidKey);
                    List<Income> list = new ArrayList<>();
                    ConfigurationSection sec = iRoot.getConfigurationSection(uuidKey);
                    if (sec != null) {
                        for (String key : sec.getKeys(false)) {
                            ConfigurationSection item = sec.getConfigurationSection(key);
                            if (item == null) {
                                continue;
                            }
                            list.add(new Income(item.getLong("time", 0),
                                    item.getString("source", ""),
                                    item.getDouble("amount", 0),
                                    item.getString("from", "")));
                        }
                    }
                    incomes.put(uuid, list);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (int i = 0; i < transfers.size(); i++) {
            Transfer t = transfers.get(i);
            String base = "transfers." + i + ".";
            config.set(base + "sender", t.getSender().toString());
            config.set(base + "receiver", t.getReceiver().toString());
            config.set(base + "amount", t.getAmount());
            config.set(base + "note", t.getNote());
            config.set(base + "create-time", t.getCreateTime());
            config.set(base + "expire-time", t.getExpireTime());
        }
        for (int i = 0; i < loans.size(); i++) {
            Loan l = loans.get(i);
            String base = "loans." + i + ".";
            config.set(base + "borrower", l.getBorrower().toString());
            config.set(base + "lender", l.getLender().toString());
            config.set(base + "amount", l.getAmount());
            config.set(base + "note", l.getNote());
            config.set(base + "interest-rate", l.getInterestRate());
            config.set(base + "days", l.getDays());
            config.set(base + "status", l.getStatus().name());
            config.set(base + "create-time", l.getCreateTime());
            config.set(base + "due-time", l.getDueTime());
            config.set(base + "repaid", l.getRepaidAmount());
        }
        for (int i = 0; i < debts.size(); i++) {
            Debt d = debts.get(i);
            String base = "debts." + i + ".";
            config.set(base + "borrower", d.getBorrower().toString());
            config.set(base + "lender", d.getLender().toString());
            config.set(base + "principal", d.getPrincipal());
            config.set(base + "interest-rate", d.getInterestRate());
            config.set(base + "due-time", d.getDueTime());
            config.set(base + "last-penalty-time", d.getLastPenaltyTime());
        }
        for (Map.Entry<UUID, List<Income>> entry : incomes.entrySet()) {
            String base = "incomes." + entry.getKey().toString() + ".";
            List<Income> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                Income income = list.get(i);
                String itemBase = base + i + ".";
                config.set(itemBase + "time", income.getTime());
                config.set(itemBase + "source", income.getSource());
                config.set(itemBase + "amount", income.getAmount());
                config.set(itemBase + "from", income.getFrom());
            }
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("保存经济数据失败: " + e.getMessage());
        }
    }

    public Transfer createTransfer(UUID sender, UUID receiver, double amount, String note) {
        long now = System.currentTimeMillis();
        long expire = now + plugin.getConfigManager().getTransferExpireDays() * 24L * 60 * 60 * 1000;
        Transfer transfer = new Transfer(sender, receiver, amount, note, now, expire);
        transfers.add(transfer);
        save();
        return transfer;
    }

    public List<Transfer> getPendingTransfers(UUID receiver) {
        List<Transfer> result = new ArrayList<>();
        for (Transfer t : transfers) {
            if (t.getReceiver().equals(receiver) && !t.isExpired()) {
                result.add(t);
            }
        }
        return result;
    }

    public boolean claimTransfer(Transfer transfer) {
        if (transfer == null || transfer.isExpired()) {
            return false;
        }
        plugin.getEconomyManager().getProvider().deposit(
                Bukkit.getOfflinePlayer(transfer.getReceiver()), transfer.getAmount());
        recordIncome(transfer.getReceiver(), "转账领取",
                transfer.getAmount(), Bukkit.getOfflinePlayer(transfer.getSender()).getName());
        transfers.remove(transfer);
        save();
        return true;
    }

    public int processExpiredTransfers() {
        List<Transfer> expired = new ArrayList<>();
        for (Transfer t : transfers) {
            if (t.isExpired()) {
                expired.add(t);
            }
        }
        for (Transfer t : expired) {
            plugin.getEconomyManager().getProvider().deposit(
                    Bukkit.getOfflinePlayer(t.getSender()), t.getAmount());
            transfers.remove(t);
            Player sender = Bukkit.getPlayer(t.getSender());
            if (sender != null && sender.isOnline()) {
                sender.sendMessage(TextUtil.color("&e你转出的 &f"
                        + TextUtil.formatMoney(t.getAmount()) + " &e因过期已退回。"));
            }
        }
        if (!expired.isEmpty()) {
            save();
        }
        return expired.size();
    }

    public Loan createLoan(UUID borrower, UUID lender, double amount, String note) {
        Loan loan = new Loan(borrower, lender, amount, note, System.currentTimeMillis());
        loans.add(loan);
        save();
        return loan;
    }

    public List<Loan> getActiveLoansOf(UUID borrower) {
        List<Loan> result = new ArrayList<>();
        for (Loan l : loans) {
            if (l.getBorrower().equals(borrower) && l.getStatus() == Loan.Status.ACTIVE) {
                result.add(l);
            }
        }
        return result;
    }

    public Loan findPendingLoan(UUID borrower, UUID lender) {
        for (Loan l : loans) {
            if (l.getBorrower().equals(borrower) && l.getLender().equals(lender)
                    && l.getStatus() == Loan.Status.PENDING) {
                return l;
            }
        }
        return null;
    }

    public boolean acceptLoan(Loan loan, int days, double interestRate) {
        if (loan == null || loan.getStatus() != Loan.Status.PENDING) {
            return false;
        }
        if (!plugin.getEconomyManager().getProvider().has(
                Bukkit.getOfflinePlayer(loan.getLender()), loan.getAmount())) {
            return false;
        }
        plugin.getEconomyManager().getProvider().withdraw(
                Bukkit.getOfflinePlayer(loan.getLender()), loan.getAmount());
        plugin.getEconomyManager().getProvider().deposit(
                Bukkit.getOfflinePlayer(loan.getBorrower()), loan.getAmount());
        recordIncome(loan.getBorrower(), "借款放款",
                loan.getAmount(), Bukkit.getOfflinePlayer(loan.getLender()).getName());
        loan.setDays(days);
        loan.setInterestRate(interestRate);
        loan.setDueTime(System.currentTimeMillis() + days * 24L * 60 * 60 * 1000);
        loan.setStatus(Loan.Status.ACTIVE);
        save();
        return true;
    }

    public boolean rejectLoan(Loan loan) {
        if (loan == null || loan.getStatus() != Loan.Status.PENDING) {
            return false;
        }
        loans.remove(loan);
        save();
        return true;
    }

    public boolean repayLoan(Loan loan, double amount) {
        if (loan == null || loan.getStatus() != Loan.Status.ACTIVE) {
            return false;
        }
        double pay = Math.min(amount, loan.getRemaining());
        if (!plugin.getEconomyManager().getProvider().has(
                Bukkit.getOfflinePlayer(loan.getBorrower()), pay)) {
            return false;
        }
        plugin.getEconomyManager().getProvider().withdraw(
                Bukkit.getOfflinePlayer(loan.getBorrower()), pay);
        plugin.getEconomyManager().getProvider().deposit(
                Bukkit.getOfflinePlayer(loan.getLender()), pay);
        recordIncome(loan.getLender(), "借款还款",
                pay, Bukkit.getOfflinePlayer(loan.getBorrower()).getName());
        loan.setRepaidAmount(loan.getRepaidAmount() + pay);
        if (loan.getRemaining() <= 0.001) {
            loan.setStatus(Loan.Status.REPAID);
        }
        save();
        return true;
    }

    public int processDueLoans() {
        int count = 0;
        for (Loan loan : new ArrayList<>(loans)) {
            if (!loan.isDue()) {
                continue;
            }
            double remaining = loan.getRemaining();
            if (plugin.getEconomyManager().getProvider().has(
                    Bukkit.getOfflinePlayer(loan.getBorrower()), remaining)) {
                plugin.getEconomyManager().getProvider().withdraw(
                        Bukkit.getOfflinePlayer(loan.getBorrower()), remaining);
                plugin.getEconomyManager().getProvider().deposit(
                        Bukkit.getOfflinePlayer(loan.getLender()), remaining);
                recordIncome(loan.getLender(), "借款到期收款",
                        remaining, Bukkit.getOfflinePlayer(loan.getBorrower()).getName());
                loan.setRepaidAmount(loan.getTotalDue());
                loan.setStatus(Loan.Status.SETTLED);
                notifySettled(loan, remaining);
            } else {
                debts.add(new Debt(loan.getBorrower(), loan.getLender(),
                        loan.getAmount(), loan.getInterestRate(),
                        loan.getDueTime(), System.currentTimeMillis()));
                loan.setStatus(Loan.Status.SETTLED);
                Player borrower = Bukkit.getPlayer(loan.getBorrower());
                if (borrower != null && borrower.isOnline()) {
                    borrower.sendMessage(TextUtil.color("&c你的借款已到期，余额不足，已记录欠款 &f"
                            + TextUtil.formatMoney(remaining) + "&c，逾期每天额外 &f"
                            + TextUtil.formatMoney(plugin.getConfigManager().getOverduePenaltyPerDay())));
                }
            }
            count++;
        }
        if (count > 0) {
            save();
        }
        return count;
    }

    private void notifySettled(Loan loan, double amount) {
        Player borrower = Bukkit.getPlayer(loan.getBorrower());
        if (borrower != null && borrower.isOnline()) {
            borrower.sendMessage(TextUtil.color("&c你的借款已到期，被强制收款 &f"
                    + TextUtil.formatMoney(amount)));
        }
        Player lender = Bukkit.getPlayer(loan.getLender());
        if (lender != null && lender.isOnline()) {
            lender.sendMessage(TextUtil.color("&a你的借款已到期，收到还款 &f"
                    + TextUtil.formatMoney(amount)));
        }
    }

    public void processLoanReminders() {
        for (Loan loan : loans) {
            if (!loan.isNearDue()) {
                continue;
            }
            Player borrower = Bukkit.getPlayer(loan.getBorrower());
            if (borrower != null && borrower.isOnline()) {
                long hours = (loan.getDueTime() - System.currentTimeMillis()) / (60 * 60 * 1000);
                borrower.sendMessage(TextUtil.color("&e你的借款将在 &f" + hours
                        + " &e小时后到期，应还 &f" + TextUtil.formatMoney(loan.getRemaining())));
            }
        }
    }

    public void checkDebt(UUID player) {
        List<Debt> toRemove = new ArrayList<>();
        for (Debt debt : debts) {
            if (!debt.getBorrower().equals(player)) {
                continue;
            }
            double owed = debt.getTotalOwed(plugin.getConfigManager().getOverduePenaltyPerDay());
            if (plugin.getEconomyManager().getProvider().has(
                    Bukkit.getOfflinePlayer(player), owed)) {
                plugin.getEconomyManager().getProvider().withdraw(
                        Bukkit.getOfflinePlayer(player), owed);
                plugin.getEconomyManager().getProvider().deposit(
                        Bukkit.getOfflinePlayer(debt.getLender()), owed);
                recordIncome(debt.getLender(), "欠款追回",
                        owed, Bukkit.getOfflinePlayer(player).getName());
                toRemove.add(debt);
                Player borrower = Bukkit.getPlayer(player);
                if (borrower != null && borrower.isOnline()) {
                    borrower.sendMessage(TextUtil.color("&c你的欠款 &f" + TextUtil.formatMoney(owed)
                            + " &c已自动扣除并支付给放款方。"));
                }
            }
        }
        if (!toRemove.isEmpty()) {
            debts.removeAll(toRemove);
            save();
        }
    }

    public List<Debt> getDebtsOf(UUID borrower) {
        List<Debt> result = new ArrayList<>();
        for (Debt d : debts) {
            if (d.getBorrower().equals(borrower)) {
                result.add(d);
            }
        }
        return result;
    }

    public void recordIncome(UUID player, String source, double amount, String from) {
        List<Income> list = incomes.computeIfAbsent(player, k -> new ArrayList<>());
        list.add(new Income(System.currentTimeMillis(), source, amount, from == null ? "" : from));
        int max = plugin.getConfigManager().getMaxIncomeRecords();
        while (list.size() > max) {
            list.remove(0);
        }
    }

    public List<Income> getIncomes(UUID player) {
        List<Income> list = incomes.get(player);
        if (list == null) {
            return new ArrayList<>();
        }
        List<Income> result = new ArrayList<>(list);
        Collections.reverse(result);
        return result;
    }
}
