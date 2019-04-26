#
# This Source Code Form is subject to the terms of the Mozilla Public License, v.
# 2.0 with a Healthcare Disclaimer.
#
# A copy of the Mozilla Public License, v. 2.0 with the Healthcare Disclaimer can
# be found under the top level directory, named LICENSE.
#
# If a copy of the MPL was not distributed with this file, You can obtain one at
# http://mozilla.org/MPL/2.0/.
#
# If a copy of the Healthcare Disclaimer was not distributed with this file, You
# can obtain one at the project website https://github.com/persistentsystems/open-cpra-calculator.
#
# Copyright (C) 2016-2018 Persistent Systems, Inc.
#

if (! docker network ls -f 'name=dockercompose_default' | grep -q 'dockercompose_default'); then
	docker network create dockercompose_default --subnet 192.168.201.1/24
elif (docker network inspect dockercompose_default | grep Subnet | grep -q '192.168.201.1'); then
	echo "Network dockercompose_default exists."
else
	echo "!!!!!!!  Warning !!!!!!!"
	echo "Network dockercompose_default exist but with wrong configuration. Please delete and recreated it."
fi
