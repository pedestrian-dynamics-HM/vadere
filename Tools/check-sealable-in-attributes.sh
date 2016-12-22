#!/bin/bash
# In all attributes classes: check if there are setters with a first line other than checkSealed()
# Call this program from the repository root!

# $1: error message
exitWithError() {
    echo "$1" >&2
    exit 1
}

main() {
    declare file dir=VadereState/src/org/vadere/state/attributes/
    [[ -d $dir ]] || exitWithError "error: dir does not exist"
    find "$dir" -name 'Attributes*.java' | while read file; do
        awk -v file="$file" 's{if($0 !~ /checkSealed()/) print s; s=""}; /void +set/{s=file ":" NR " " $0}' "$file"
    done
}

main
