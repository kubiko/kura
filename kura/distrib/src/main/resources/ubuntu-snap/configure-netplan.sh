#!/bin/sh

if [ "${1}" = "setup"  ]; then
    # try configuring netplan, in case we did not have permission before
    if [ ! -f /etc/netplan/00-default-nm-renderer.yaml \
           -o "x$(grep kura /etc/netplan/00-default-nm-renderer.yaml)" = "x" ]; then
        # first try to remove network manager config
        if [ -f /etc/netplan/01-network-manager-all.yaml ]; then
            mv /etc/netplan/01-network-manager-all.yaml ${SNAP_COMMON}/01-network-manager-all.yaml.back
        fi
        if [ -f /etc/netplan/00-default-nm-renderer.yaml ]; then
            mv /etc/netplan/00-default-nm-renderer.yaml ${SNAP_COMMON}/00-default-nm-renderer.yaml.back
        fi
        cp ${SNAP}/kura/install/00-default-nm-renderer.yaml /etc/netplan/
        netplan generate
        netplan apply
    fi
fi

if [ "${1}" = "remove" ]; then
    # if we have backup, try to restore previous network configuration
    if [ -f ${SNAP_COMMON}/01-network-manager-all.yaml.back ]; then
        cp ${SNAP_COMMON}/01-network-manager-all.yaml.back /etc/netplan/01-network-manager-all.yaml
    fi
    rm /etc/netplan/00-default-nm-renderer.yaml
    if [ -f ${SNAP_COMMON}/00-default-nm-renderer.yaml.back ]; then
        cp ${SNAP_COMMON}/01-network-manager-all.yaml.back /etc/netplan/00-default-nm-renderer.yaml
    fi
    netplan generate
    netplan apply
fi

