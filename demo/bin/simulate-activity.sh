#!/bin/bash
#
# Copyright 2013 Cloudera Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

USER_ID=$1
REPEATS=${2:-100000}
COOKIE_FILE=/tmp/user_${USER_ID}_cookies.txt
rm -f $COOKIE_FILE

for (( i=1; i<=$REPEATS; i++ ))
do
  curl -sL -w "%{http_code} %{url_effective}\\n" \
      -o /dev/null \
      -b $COOKIE_FILE \
      -c $COOKIE_FILE \
      "http://localhost:8080/demo-webapp/send?user_id=$USER_ID&message=message$i"
  sleep $(printf ".%02ds" $(( $RANDOM % 1000 )))
done