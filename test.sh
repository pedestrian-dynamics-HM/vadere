#!/usr/bin/env bash
SCRIPT="whoami; ls -l"
ssh -p 5022 dlehmberg@minimuc.cs.hm.edu "${SCRIPT}"

