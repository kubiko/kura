package org.eclipse.kura.linux.net.iptables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.linux.net.util.KuraConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptablesConfig {

    private static final Logger s_logger = LoggerFactory.getLogger(IptablesConfig.class);
    private static final boolean IS_UBUNTU_CORE = System.getProperty("kura.os.version").equals(KuraConstants.UbuntuCore.getImageName());
    private static final String SNAP_COMMON = System.getProperty("kura.data.dir") + "/..";
    private static final String SNAP_DATA = System.getProperty("kura.data.dir") + "/../../current";
    public static final String FIREWALL_CONFIG_FILE_NAME = IS_UBUNTU_CORE ? SNAP_COMMON + "/etc/sysconfig/iptables" : "/etc/sysconfig/iptables";
    public static final String FIREWALL_TMP_CONFIG_FILE_NAME = IS_UBUNTU_CORE ? SNAP_DATA + "/tmp/iptables" : "/tmp/iptables";

    private static final String ALLOW_ALL_TRAFFIC_TO_LOOPBACK = "-A INPUT -i lo -j ACCEPT";
    private static final String ALLOW_ONLY_INCOMING_TO_OUTGOING = "-A INPUT -m state --state RELATED,ESTABLISHED -j ACCEPT";

    private static final String[] ALLOW_ICMP = {
            "-A INPUT -p icmp -m icmp --icmp-type 8 -m state --state NEW,RELATED,ESTABLISHED -j ACCEPT",
            "-A OUTPUT -p icmp -m icmp --icmp-type 0 -m state --state RELATED,ESTABLISHED -j ACCEPT" };

    private static final String[] DO_NOT_ALLOW_ICMP = {
            "-A INPUT -p icmp -m icmp --icmp-type 8 -m state --state NEW,RELATED,ESTABLISHED -j DROP",
            "-A OUTPUT -p icmp -m icmp --icmp-type 0 -m state --state RELATED,ESTABLISHED -j DROP" };

    private final LinkedHashSet<LocalRule> m_localRules;
    private final LinkedHashSet<PortForwardRule> m_portForwardRules;
    private final LinkedHashSet<NATRule> m_autoNatRules;
    private final LinkedHashSet<NATRule> m_natRules;
    private boolean m_allowIcmp;

    public IptablesConfig() {
        this.m_localRules = new LinkedHashSet<LocalRule>();
        this.m_portForwardRules = new LinkedHashSet<PortForwardRule>();
        this.m_autoNatRules = new LinkedHashSet<NATRule>();
        this.m_natRules = new LinkedHashSet<NATRule>();
    }

    public IptablesConfig(LinkedHashSet<LocalRule> localRules, LinkedHashSet<PortForwardRule> portForwardRules,
            LinkedHashSet<NATRule> autoNatRules, LinkedHashSet<NATRule> natRules, boolean allowIcmp) {
        this.m_localRules = localRules;
        this.m_portForwardRules = portForwardRules;
        this.m_autoNatRules = autoNatRules;
        this.m_natRules = natRules;
        this.m_allowIcmp = allowIcmp;
    }

    /*
     * Clears all chains
     */
    public static void clearAllChains() throws KuraException {
        FileOutputStream fos = null;
        PrintWriter writer = null;
        try {
            fos = new FileOutputStream(FIREWALL_TMP_CONFIG_FILE_NAME);
            writer = new PrintWriter(fos);
            writer.println("*nat");
            writer.println("COMMIT");
            writer.println("*filter");
            writer.println("COMMIT");
        } catch (Exception e) {
            s_logger.error("clear() :: failed to clear all chains - {}", e);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    s_logger.error("clear() :: failed to close FileOutputStream - {}", e);
                }
            }
        }
        File configFile = new File(FIREWALL_TMP_CONFIG_FILE_NAME);
        if (configFile.exists()) {
            restore(FIREWALL_TMP_CONFIG_FILE_NAME);
        }
    }

    public static void applyBlockPolicy() throws KuraException {
        FileOutputStream fos = null;
        PrintWriter writer = null;
        try {
            fos = new FileOutputStream(FIREWALL_TMP_CONFIG_FILE_NAME);
            writer = new PrintWriter(fos);
            writer.println("*nat");
            writer.println("COMMIT");
            writer.println("*filter");
            writer.println(ALLOW_ALL_TRAFFIC_TO_LOOPBACK);
            writer.println(ALLOW_ONLY_INCOMING_TO_OUTGOING);
            writer.println("COMMIT");
        } catch (Exception e) {
            s_logger.error("clear() :: failed to clear all chains - {}", e);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    s_logger.error("clear() :: failed to close FileOutputStream - {}", e);
                }
            }
        }
        File configFile = new File(FIREWALL_TMP_CONFIG_FILE_NAME);
        if (configFile.exists()) {
            restore(FIREWALL_TMP_CONFIG_FILE_NAME);
        }
    }

    /*
     * Saves (using iptables-save) the current iptables config into /etc/sysconfig/iptables
     */
    public static void save() throws KuraException {
        SafeProcess proc = null;
        BufferedReader br = null;
        PrintWriter out = null;
        try {
            int status = -1;
            proc = ProcessUtil.exec("iptables-save");
            status = proc.waitFor();
            if (status != 0) {
                s_logger.error("save() :: failed - {}", LinuxProcessUtil.getInputStreamAsString(proc.getErrorStream()));
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Failed to execute the iptable-save command");
            }
            String line = null;
            br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            out = new PrintWriter(FIREWALL_CONFIG_FILE_NAME);
            while ((line = br.readLine()) != null) {
                out.println(line);
            }
            s_logger.debug("iptablesSave() :: completed!, status={}", status);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    s_logger.error("iptablesSave() :: failed to close BufferedReader - {}", e);
                }
            }
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    /*
     * Restores (using iptables-restore) firewall settings from temporary iptables configuration file.
     * Temporary configuration file is deleted upon completion.
     */
    public static void restore(String filename) throws KuraException {
        SafeProcess proc = null;
        try {
            proc = ProcessUtil.exec("iptables-restore " + filename);
            int status = proc.waitFor();
            if (status != 0) {
                s_logger.error("restore() :: failed - {}",
                        LinuxProcessUtil.getInputStreamAsString(proc.getErrorStream()));
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, "Failed to execute the iptable-restore command");
            }
        } catch (Exception e) {
            s_logger.error("restore() :: exception={}", e);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
            File configFile = new File(filename);
            if (configFile.exists()) {
                configFile.delete();
            }
        }
    }

    /*
     * Saves current configurations from the m_localRules, m_portForwardRules, m_natRules, and m_autoNatRules
     * into specified temporary file
     */
    public void save(String filename) throws KuraException {
        FileOutputStream fos = null;
        PrintWriter writer = null;
        try {
            fos = new FileOutputStream(FIREWALL_TMP_CONFIG_FILE_NAME);
            writer = new PrintWriter(fos);
            writer.println("*filter");
            writer.println(ALLOW_ALL_TRAFFIC_TO_LOOPBACK);
            writer.println(ALLOW_ONLY_INCOMING_TO_OUTGOING);
            if (this.m_allowIcmp) {
                for (String sAllowIcmp : ALLOW_ICMP) {
                    writer.println(sAllowIcmp);
                }
            } else {
                for (String sDoNotAllowIcmp : DO_NOT_ALLOW_ICMP) {
                    writer.println(sDoNotAllowIcmp);
                }
            }
            if (this.m_localRules != null && !this.m_localRules.isEmpty()) {
                for (LocalRule lr : this.m_localRules) {
                    writer.println(lr);
                }
            }
            if (this.m_portForwardRules != null && !this.m_portForwardRules.isEmpty()) {
                for (PortForwardRule portForwardRule : this.m_portForwardRules) {
                    List<String> filterForwardChainRules = portForwardRule.getFilterForwardChainRule().toStrings();
                    if (filterForwardChainRules != null && !filterForwardChainRules.isEmpty()) {
                        for (String filterForwardChainRule : filterForwardChainRules) {
                            writer.println(filterForwardChainRule);
                        }
                    }
                }
            }
            if (this.m_autoNatRules != null && !this.m_autoNatRules.isEmpty()) {
                for (NATRule autoNatRule : this.m_autoNatRules) {
                    List<String> filterForwardChainRules = autoNatRule.getFilterForwardChainRule().toStrings();
                    if (filterForwardChainRules != null && !filterForwardChainRules.isEmpty()) {
                        for (String filterForwardChainRule : filterForwardChainRules) {
                            writer.println(filterForwardChainRule);
                        }
                    }
                }
            }
            if (this.m_natRules != null && !this.m_natRules.isEmpty()) {
                for (NATRule natRule : this.m_natRules) {
                    List<String> filterForwardChainRules = natRule.getFilterForwardChainRule().toStrings();
                    if (filterForwardChainRules != null && !filterForwardChainRules.isEmpty()) {
                        for (String filterForwardChainRule : filterForwardChainRules) {
                            writer.println(filterForwardChainRule);
                        }
                    }
                }
            }
            writer.println("COMMIT");
            writer.println("*nat");
            if (this.m_portForwardRules != null && !this.m_portForwardRules.isEmpty()) {
                for (PortForwardRule portForwardRule : this.m_portForwardRules) {
                    writer.println(portForwardRule.getNatPreroutingChainRule());
                    writer.println(portForwardRule.getNatPostroutingChainRule());
                }
            }
            if (this.m_autoNatRules != null && !this.m_autoNatRules.isEmpty()) {
                List<NatPostroutingChainRule> appliedNatPostroutingChainRules = new ArrayList<NatPostroutingChainRule>();
                for (NATRule autoNatRule : this.m_autoNatRules) {
                    boolean found = false;
                    NatPostroutingChainRule natPostroutingChainRule = autoNatRule.getNatPostroutingChainRule();

                    for (NatPostroutingChainRule appliedNatPostroutingChainRule : appliedNatPostroutingChainRules) {

                        if (appliedNatPostroutingChainRule.equals(natPostroutingChainRule)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        writer.println(autoNatRule.getNatPostroutingChainRule());
                        appliedNatPostroutingChainRules.add(natPostroutingChainRule);
                    }
                }
            }
            if (this.m_natRules != null && !this.m_natRules.isEmpty()) {
                for (NATRule natRule : this.m_natRules) {
                    writer.println(natRule.getNatPostroutingChainRule());
                }
            }
            writer.println("COMMIT");
        } catch (Exception e) {
            s_logger.error("clear() :: failed to clear all chains - {}", e);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    s_logger.error("clear() :: failed to close FileOutputStream - {}", e);
                }
            }
        }
    }

    /*
     * Populates the m_localRules, m_portForwardRules, m_natRules, and m_autoNatRules by parsing
     * the iptables configuration file.
     */
    public void restore() throws KuraException {
        BufferedReader br = null;
        try {
            List<NatPreroutingChainRule> natPreroutingChain = new ArrayList<NatPreroutingChainRule>();
            List<NatPostroutingChainRule> natPostroutingChain = new ArrayList<NatPostroutingChainRule>();
            List<FilterForwardChainRule> filterForwardChain = new ArrayList<FilterForwardChainRule>();

            br = new BufferedReader(new FileReader(FIREWALL_CONFIG_FILE_NAME));
            String line = null;
            boolean readingNatTable = false;
            boolean readingFilterTable = false;
            lineloop: while ((line = br.readLine()) != null) {
                line = line.trim();
                // skip any predefined lines or comment lines
                if (line.equals("")) {
                    continue;
                }
                if (line.startsWith("#") || line.startsWith(":")) {
                    continue;
                }
                if (line.equals("*nat")) {
                    readingNatTable = true;
                } else if (line.equals("*filter")) {
                    readingFilterTable = true;
                } else if (line.equals("COMMIT")) {
                    if (readingNatTable) {
                        readingNatTable = false;
                    }
                    if (readingFilterTable) {
                        readingFilterTable = false;
                    }
                } else if (readingNatTable && line.startsWith("-A PREROUTING")) {
                    natPreroutingChain.add(new NatPreroutingChainRule(line));
                } else if (readingNatTable && line.startsWith("-A POSTROUTING")) {
                    natPostroutingChain.add(new NatPostroutingChainRule(line));
                } else if (readingFilterTable && line.startsWith("-A FORWARD")) {
                    filterForwardChain.add(new FilterForwardChainRule(line));
                } else if (readingFilterTable && line.startsWith("-A INPUT")) {
                    if (ALLOW_ALL_TRAFFIC_TO_LOOPBACK.equals(line)) {
                        continue;
                    }
                    if (ALLOW_ONLY_INCOMING_TO_OUTGOING.equals(line)) {
                        continue;
                    }
                    for (String allowIcmp : ALLOW_ICMP) {
                        if (allowIcmp.equals(line)) {
                            this.m_allowIcmp = true;
                            continue lineloop;
                        }
                    }
                    for (String allowIcmp : DO_NOT_ALLOW_ICMP) {
                        if (allowIcmp.equals(line)) {
                            this.m_allowIcmp = false;
                            continue lineloop;
                        }
                    }
                    try {
                        LocalRule localRule = new LocalRule(line);
                        s_logger.debug("parseFirewallConfigurationFile() :: Adding local rule: {}", localRule);
                        this.m_localRules.add(localRule);
                    } catch (KuraException e) {
                        s_logger.error("Failed to parse Local Rule: {} - {}", line, e);
                    }
                }
            }

            // ! done parsing !
            for (NatPreroutingChainRule natPreroutingChainRule : natPreroutingChain) {
                // found port forwarding rule ...
                String inboundIfaceName = natPreroutingChainRule.getInputInterface();
                String outboundIfaceName = null;
                String protocol = natPreroutingChainRule.getProtocol();
                int inPort = natPreroutingChainRule.getExternalPort();
                int outPort = natPreroutingChainRule.getInternalPort();
                boolean masquerade = false;
                String sport = null;
                if (natPreroutingChainRule.getSrcPortFirst() > 0
                        && natPreroutingChainRule.getSrcPortFirst() <= natPreroutingChainRule.getSrcPortLast()) {
                    StringBuilder sbSport = new StringBuilder().append(natPreroutingChainRule.getSrcPortFirst())
                            .append(':').append(natPreroutingChainRule.getSrcPortLast());
                    sport = sbSport.toString();
                }
                String permittedMac = natPreroutingChainRule.getPermittedMacAddress();
                String permittedNetwork = natPreroutingChainRule.getPermittedNetwork();
                int permittedNetworkMask = natPreroutingChainRule.getPermittedNetworkMask();
                String address = natPreroutingChainRule.getDstIpAddress();

                for (NatPostroutingChainRule natPostroutingChainRule : natPostroutingChain) {
                    if (natPreroutingChainRule.getDstIpAddress().equals(natPostroutingChainRule.getDstNetwork())) {
                        outboundIfaceName = natPostroutingChainRule.getDstInterface();
                        if (natPostroutingChainRule.isMasquerade()) {
                            masquerade = true;
                        }
                    }
                }
                if (permittedNetwork == null) {
                    permittedNetwork = "0.0.0.0";
                }
                PortForwardRule portForwardRule = new PortForwardRule(inboundIfaceName, outboundIfaceName, address,
                        protocol, inPort, outPort, masquerade, permittedNetwork, permittedNetworkMask, permittedMac,
                        sport);
                s_logger.debug("Adding port forward rule: {}", portForwardRule);
                this.m_portForwardRules.add(portForwardRule);
            }

            for (NatPostroutingChainRule natPostroutingChainRule : natPostroutingChain) {
                String destinationInterface = natPostroutingChainRule.getDstInterface();
                boolean masquerade = natPostroutingChainRule.isMasquerade();
                String protocol = natPostroutingChainRule.getProtocol();
                if (protocol != null) {
                    // found NAT rule, ... maybe
                    boolean isNATrule = false;
                    String source = natPostroutingChainRule.getSrcNetwork();
                    String destination = natPostroutingChainRule.getDstNetwork();
                    if (destination != null) {
                        StringBuilder sbDestination = new StringBuilder().append(destination).append('/')
                                .append(natPostroutingChainRule.getDstMask());
                        destination = sbDestination.toString();
                    } else {
                        isNATrule = true;
                    }

                    if (source != null) {
                        StringBuilder sbSource = new StringBuilder().append(source).append('/')
                                .append(natPostroutingChainRule.getSrcMask());
                        source = sbSource.toString();
                    }
                    if (!isNATrule) {
                        boolean matchFound = false;
                        for (NatPreroutingChainRule natPreroutingChainRule : natPreroutingChain) {
                            if (natPreroutingChainRule.getDstIpAddress()
                                    .equals(natPostroutingChainRule.getDstNetwork())) {
                                matchFound = true;
                                break;
                            }
                        }
                        if (!matchFound) {
                            isNATrule = true;
                        }
                    }
                    if (isNATrule) {
                        // match FORWARD rule to find out source interface ...
                        for (FilterForwardChainRule filterForwardChainRule : filterForwardChain) {
                            if (natPostroutingChainRule.isMatchingForwardChainRule(filterForwardChainRule)) {
                                String sourceInterface = filterForwardChainRule.getInputInterface();
                                s_logger.debug("parseFirewallConfigurationFile() :: Parsed NAT rule with"
                                        + "   sourceInterface: " + sourceInterface + "   destinationInterface: "
                                        + destinationInterface + "   masquerade: " + masquerade + "	protocol: "
                                        + protocol + "	source network/host: " + source + "	destination network/host "
                                        + destination);
                                NATRule natRule = new NATRule(sourceInterface, destinationInterface, protocol, source,
                                        destination, masquerade);
                                s_logger.debug("parseFirewallConfigurationFile() :: Adding NAT rule {}", natRule);
                                this.m_natRules.add(natRule);
                            }
                        }
                    }
                } else {
                    // found Auto NAT rule ...
                    // match FORWARD rule to find out source interface ...
                    for (FilterForwardChainRule filterForwardChainRule : filterForwardChain) {
                        if (natPostroutingChainRule.isMatchingForwardChainRule(filterForwardChainRule)) {
                            String sourceInterface = filterForwardChainRule.getInputInterface();
                            s_logger.debug("parseFirewallConfigurationFile() :: Parsed auto NAT rule with"
                                    + "   sourceInterface: " + sourceInterface + "   destinationInterface: "
                                    + destinationInterface + "   masquerade: " + masquerade);

                            NATRule natRule = new NATRule(sourceInterface, destinationInterface, masquerade);
                            s_logger.debug("parseFirewallConfigurationFile() :: Adding auto NAT rule {}", natRule);
                            this.m_autoNatRules.add(natRule);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            // close
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                }
                br = null;
            }
        }
    }

    public LinkedHashSet<LocalRule> getLocalRules() {
        return this.m_localRules;
    }

    public LinkedHashSet<PortForwardRule> getPortForwardRules() {
        return this.m_portForwardRules;
    }

    public LinkedHashSet<NATRule> getAutoNatRules() {
        return this.m_autoNatRules;
    }

    public LinkedHashSet<NATRule> getNatRules() {
        return this.m_natRules;
    }

    public boolean allowIcmp() {
        return this.m_allowIcmp;
    }
}
