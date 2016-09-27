package org.onosproject.evpn.rsc;

import java.util.List;
import java.util.Objects;

import org.onosproject.vtnrsc.VirtualPort;

public class EvpnInstance {
    private String name;
    private String id;
    private String description;
    private String rd;
    private String rt;
    private List<VirtualPort> portlist;

    public EvpnInstance(String name, String id, String description, String rd,
                        String rt, List<VirtualPort> portlist) {
        super();
        this.name = name;
        this.id = id;
        this.description = description;
        this.rd = rd;
        this.rt = rt;
        this.portlist = portlist;
    }

    public String name() {
        return name;
    }

    public String id() {
        return id;
    }

    public String description() {
        return description;
    }

    public String rd() {
        return rd;
    }

    public String rt() {
        return rt;
    }

    public List<VirtualPort> portlist() {
        return portlist;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, description, rd, rt, portlist);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EvpnInstance) {
            final EvpnInstance that = (EvpnInstance) obj;
            return Objects.equals(this.name, that.name)
                    && Objects.equals(this.id, that.id)
                    && Objects.equals(this.description, that.description)
                    && Objects.equals(this.rd, that.rt)
                    && Objects.equals(this.portlist, that.portlist);
        }
        return false;
    }
}
