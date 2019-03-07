#!/bin/sh
#
# Copyright (c) 2011, 2018 Eurotech and/or its affiliates
#
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   Canonical
#


# clean any existing run dirs and link new ones
rm -rf ${SNAP_DATA}/run ${SNAP_DATA}/var/run ${SNAP_COMMON}/run ${SNAP_COMMON}/var/run
mkdir -p ${SNAP_DATA}/run
# setup sym links for run dir
ln -sf ${SNAP_DATA}/run ${SNAP_DATA}/var/run
ln -sf ${SNAP_DATA}/run ${SNAP_COMMON}/run
ln -sf ${SNAP_DATA}/run ${SNAP_COMMON}/var/run

# setup writable directories
mkdir -p ${SNAP_DATA}/log
mkdir -p ${SNAP_DATA}/var
mkdir -p ${SNAP_COMMON}/data/packages
mkdir -p ${SNAP_COMMON}/.data
mkdir -p ${SNAP_COMMON}/var/log

# populate default writable data
cp -r ${SNAP}/kura/user ${SNAP_COMMON}
cp -r ${SNAP}/kura/framework ${SNAP_COMMON}
cp ${SNAP_COMMON}/user/snapshots/snapshot_0.xml ${SNAP_COMMON}/.data/snapshot_0.xml

# copy over monit configuration
cp ${SNAP}/kura/install/monitrc.snap ${SNAP_COMMON}/etc/monitrc
chmod 700 ${SNAP_COMMON}/etc/monitrc
sed -i -e 's#${SNAP_NAME}#'"${SNAP_NAME}"'#g' \
       -e 's#${SNAP_COMMON}#'"${SNAP_COMMON}"'#g' \
     ${SNAP_COMMON}/etc/monitrc

# corect paths in properties
SNAP_DATA_CURRENT=$(dirname ${SNAP_DATA})/current
find  ${SNAP_COMMON}/user ${SNAP_COMMON}/framework -type f | \
 xargs sed -i -e 's#${SNAP}#'"${SNAP}"'#g' \
              -e 's#${SNAP_NAME}#'"${SNAP_NAME}"'#g' \
              -e 's#${SNAP_COMMON}#'"${SNAP_COMMON}"'#g' \
              -e 's#${SNAP_DATA}#'"${SNAP_DATA_CURRENT}"'#g'

