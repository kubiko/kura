#!/bin/bash
#
# Copyright (c) 2011, 2019 Canonical
#
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   Canonical
#

# callback function to process one configuration
# apply_config ${key} ${env} ${value} ${default} ${description}
apply_config() {
    local val=${3}
    [ -z "${val}" ] && val=${4}
    echo "${2}=${val}"
    eval export ${2}=\"${val}\"
}

# callback function to process settings in file
# apply_settings ${key} ${file} ${conf} ${value} ${default} ${description}
apply_settings() {
    local conf=${3}
    local file=$(eval echo ${2})
    local val=${4}
    [ -z "${val}" ] && val=${5}
    if [ -f ${file} ]; then
        sed -i 's/.*'"${conf}"'.*/'"${conf}"''"${val}"'/g' ${file}
    fi
}

populate_overlayed_file() {
  if [ -z "${2}" ]; then
      source="${SNAP}/${1}"
      target="${SNAP_COMMON}/${1}"
  else
      source="${1}"
      target="${2}"
  fi
  [ "$(du ${target} | cut -c -1)" = "0" ] && cat ${source} > ${target} || true
}

# setup writable directories
mkdir -p ${SNAP_DATA}/log
mkdir -p ${SNAP_DATA}/var
mkdir -p ${SNAP_COMMON}/etc
mkdir -p ${SNAP_COMMON}/data/packages
mkdir -p ${SNAP_COMMON}/.data
mkdir -p ${SNAP_COMMON}/var/log

# clean any existing run dirs and link new ones
rm -rf ${SNAP_DATA}/run ${SNAP_DATA}/var/run ${SNAP_COMMON}/run ${SNAP_COMMON}/var/run
mkdir -p ${SNAP_DATA}/run
# setup sym links for run dir
ln -sf ${SNAP_DATA}/run ${SNAP_DATA}/var/run
ln -sf ${SNAP_DATA}/run ${SNAP_COMMON}/run
ln -sf ${SNAP_DATA}/run ${SNAP_COMMON}/var/run


# populate default writable data
cp -r --no-clobber ${SNAP}/kura/user ${SNAP_COMMON}
cp --no-clobber ${SNAP_COMMON}/user/snapshots/snapshot_0.xml ${SNAP_COMMON}/.data/snapshot_0.xml
cp -r ${SNAP}/kura/framework ${SNAP_COMMON}


# make sure monit is configured
if [ ! -f ${SNAP_COMMON}/etc/monitrc ]; then
    cp ${SNAP}/kura/install/monitrc.snap ${SNAP_COMMON}/etc/monitrc
    chmod 700 ${SNAP_COMMON}/etc/monitrc
    sed -i -e 's#${SNAP_NAME}#'"${SNAP_NAME}"'#g' \
           -e 's#${SNAP_COMMON}#'"${SNAP_COMMON}"'#g' \
           ${SNAP_COMMON}/etc/monitrc
fi

# read snap config settings
source ${SNAP}/bin/snap-config
read_snap_config

# corect paths in properties
SNAP_DATA_CURRENT=$(dirname ${SNAP_DATA})/current
find  ${SNAP_COMMON}/user ${SNAP_COMMON}/framework -type f | \
 xargs sed -i -e 's#${SNAP}#'"${SNAP}"'#g' \
              -e 's#${SNAP_NAME}#'"${SNAP_NAME}"'#g' \
              -e 's#${SNAP_COMMON}#'"${SNAP_COMMON}"'#g' \
              -e 's#${SNAP_DATA}#'"${SNAP_DATA_CURRENT}"'#g'

# clean existing logs from previous revision
[ -f ${SNAP_DATA}/log/* ] && rm ${SNAP_DATA}/log/*

if [ "${KURA_NET}" = "true" ] && [ -n "$(ls /etc/netplan/*.yaml)" ]; then
    echo "Setting up network configuration"

    # network configuration
    mkdir -p ${SNAP_DATA}/run/named

    # #set up default networking file
    # TO-DO: take this from netplan config
    cp -r ${SNAP}/etc/network ${SNAP_COMMON}/etc/
    cp ${SNAP}/kura/install/network.interfaces.snap ${SNAP_COMMON}/etc/network/interfaces
    cp ${SNAP}/kura/install/network.interfaces.snap ${SNAP_COMMON}/.data/interfaces

    #set up network helper scripts
    cp ${SNAP}/etc/network/if-up.d/ifup-local ${SNAP_COMMON}/etc/network/if-up.d/ifup-local
    cp ${SNAP}/etc/network/if-down.d/ifdown-local ${SNAP_COMMON}/etc/network/if-down.d/ifdown-local
    sed -i 's#${SNAP_COMMON}#'"$SNAP_COMMON"'#g' ${SNAP_COMMON}/etc/network/if-up.d/ifup-local


    #set up default firewall configuration
    mkdir -p ${SNAP_COMMON}/etc/sysconfig/network-scripts

    cp ${SNAP}/etc/sysconfig/iptables ${SNAP_COMMON}/etc/sysconfig/iptables
    cp ${SNAP}/etc/sysconfig/iptables ${SNAP_COMMON}/.data/iptables

    #set up networking configuration
    cp ${SNAP}/kura/install/dhcpd-eth0.conf ${SNAP_COMMON}/etc/dhcpd-eth0.conf
    cp ${SNAP}/kura/install/dhcpd-eth0.conf ${SNAP_COMMON}/.data/dhcpd-eth0.conf
    cp ${SNAP}/kura/install/dhcpd-wlan0.conf ${SNAP_COMMON}/etc/dhcpd-wlan0.conf
    cp ${SNAP}/kura/install/dhcpd-wlan0.conf ${SNAP_COMMON}/.data/dhcpd-wlan0.conf

    # TO-DO: check if this is till needed
    cp -r ${SNAP}/etc/dhcp ${SNAP_COMMON}/etc/
    mkdir -p ${SNAP_COMMON}/etc/dhcp/dhclient-enter-hooks.d
    mkdir -p ${SNAP_COMMON}/etc/dhcp/dhclient-exit-hooks.d

    cp -r ${SNAP}/etc/hostapd ${SNAP_COMMON}/etc/
    mkdir -p ${SNAP_COMMON}/etc/wpa_supplicant

    #set up kuranet.conf
    cp ${SNAP}/kura/install/kuranet.conf ${SNAP_COMMON}/user/kuranet.conf
    cp ${SNAP}/kura/install/kuranet.conf ${SNAP_COMMON}/.data/kuranet.conf


    #set up bind/named
    cp -r ${SNAP}/etc/bind ${SNAP_COMMON}/etc/
    cp ${SNAP}/kura/install/named.conf.snap     ${SNAP_COMMON}/etc/bind/named.conf
    cp ${SNAP}/kura/install/named.rfc1912.zones ${SNAP_COMMON}/etc/bind/
    mkdir -p ${SNAP_COMMON}/var/named
    cp ${SNAP}/kura/install/named.ca ${SNAP_COMMON}/var/named/
    touch ${SNAP_DATA}/log/named.log
    if [ ! -f "${SNAP_COMMON}/etc/bind/rndc.key" ]; then
        ${SNAP}/usr/sbin/rndc-confgen -r /dev/urandom -a -c ${SNAP_COMMON}/etc/bind/rndc.key
    fi
    sed -i 's#${SNAP_COMMON}#'"$SNAP_COMMON"'#g' ${SNAP_COMMON}/etc/bind/*


    mac_addr=$(head -1 /sys/class/net/eth0/address | tr '[:lower:]' '[:upper:]')
    sed "s/^ssid=kura_gateway.*/ssid=kura_gateway_${mac_addr}/" < ${SNAP}/kura/install/hostapd.conf > ${SNAP_COMMON}/etc/hostapd/hostapd-wlan0.conf
    cp ${SNAP_COMMON}/etc/hostapd/hostapd-wlan0.conf ${SNAP_COMMON}/.data/hostapd-wlan0.conf

    # try to set kura as network renderer
    # try configuring netplan, in case we have already network-configure permission
    ${SNAP}/kura/install/configure-netplan.sh setup

    # execute patch_sysctl.sh from installer install folder
    touch /etc/sysctl.d/10-kura-net.conf
    cp ${SNAP}/kura/install/sysctl.kura.conf /etc/sysctl.d/10-kura-net.conf || true
    if ! [ -d /sys/class/net ]
    then
        sysctl -p || true
    else
        sysctl -w net.ipv6.conf.all.disable_ipv6=1
        sysctl -w net.ipv6.conf.default.disable_ipv6=1
        for INTERFACE in $(ls /sys/class/net)
        do
            sysctl -w net.ipv6.conf.${INTERFACE}.disable_ipv6=1
        done
    fi

    if [ -f /etc/netplan/00-default-nm-renderer.yaml \
         -o "x$(grep kura /etc/netplan/00-default-nm-renderer.yaml)" != "x" ]; then
        snapctl set kura.net-configured=true
    fi
fi
