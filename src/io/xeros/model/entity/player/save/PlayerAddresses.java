package io.xeros.model.entity.player.save;

import java.util.Objects;

public class PlayerAddresses {

    private final String ip;
    private final String mac;
    private final String uuid;

    public PlayerAddresses(String ip, String mac, String uuid) {
        this.ip = ip;
        this.mac = mac;
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "PlayerAddresses{" +
                "ip='" + ip + '\'' +
                ", mac='" + mac + '\'' +
                ", uuid='" + uuid + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerAddresses that = (PlayerAddresses) o;
        return Objects.equals(ip, that.ip) &&
                Objects.equals(mac, that.mac) &&
                Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, mac, uuid);
    }

    public String getIp() {
        return ip;
    }

    public String getMac() {
        return mac;
    }

    public String getUUID() {
        return uuid;
    }
}
