#!/usr/bin/env bash
echo "running entrypoint.sh..."

if [ -d /files-to-evaluate ]; then
    chown -R craft:craft /files-to-evaluate
    chmod g+s /files-to-evaluate
fi

# if there are other arguments, treat them as a command to execute
if [[ -n "$@" ]];
then
    set -- gosu craft "$@"
    echo "executing as $(whoami): $@"
    exec "$@"
fi
