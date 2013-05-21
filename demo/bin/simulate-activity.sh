#!/bin/bash

USER_ID=$1
REPEATS=$2
COOKIE_FILE=/tmp/user_${USER_ID}_cookies.txt
rm -f $COOKIE_FILE

for (( i=1; i<=$REPEATS; i++ ))
do
  curl -sL -w "%{http_code} %{url_effective}\\n" \
      -o /dev/null \
      -b $COOKIE_FILE \
      -c $COOKIE_FILE \
      "http://localhost:8080/demo-webapp/send?user_id=$USER_ID&message=message$i"
  sleep $(printf ".%02ds" $(( $RANDOM % 100 )))
done