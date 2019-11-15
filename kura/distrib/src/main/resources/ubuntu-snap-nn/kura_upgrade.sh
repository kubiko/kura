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
