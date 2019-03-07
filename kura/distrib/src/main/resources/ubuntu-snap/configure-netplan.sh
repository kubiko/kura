#!/bin/sh
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

if [ "${1}" = "setup"  ]; then
    # try configuring netplan, in case we did not have permission before
    if [ ! -f /etc/netplan/00-default-nm-renderer.yaml \
           -o "x$(grep kura /etc/netplan/00-default-nm-renderer.yaml)" = "x" ]; then
        # first try to remove network manager config
        echo "Trying to configure kura as default network renderer"
        if [ -f /etc/netplan/01-network-manager-all.yaml ]; then
            echo "Backing up and removing /etc/netplan/01-network-manager-all"
            mv /etc/netplan/01-network-manager-all.yaml ${SNAP_COMMON}/01-network-manager-all.yaml.back
        fi
        if [ -f /etc/netplan/00-default-nm-renderer.yaml ]; then
            echo "Backing up and removing /etc/netplan/00-network-manager-all"
            mv /etc/netplan/00-default-nm-renderer.yaml ${SNAP_COMMON}/00-default-nm-renderer.yaml.back
        fi
        cp ${SNAP}/kura/install/00-default-nm-renderer.yaml /etc/netplan/
        netplan generate
        netplan apply
    fi
fi

if [ "${1}" = "remove" ]; then
    echo "Removing kura as default network renderer"
    # if we have backup, try to restore previous network configuration
    if [ -f ${SNAP_COMMON}/01-network-manager-all.yaml.back ]; then
        echo "Restoring network-manager as default network rendered /etc/netplan/01-network-manager-all"
        cp ${SNAP_COMMON}/01-network-manager-all.yaml.back /etc/netplan/01-network-manager-all.yaml
    fi
    rm /etc/netplan/00-default-nm-renderer.yaml
    if [ -f ${SNAP_COMMON}/00-default-nm-renderer.yaml.back ]; then
        echo "Restoring network-manager as default network rendered /etc/netplan/00-network-manager-all"
        cp ${SNAP_COMMON}/01-network-manager-all.yaml.back /etc/netplan/00-default-nm-renderer.yaml
    fi
    netplan generate
    netplan apply
fi
