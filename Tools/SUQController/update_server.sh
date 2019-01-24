SCRIPT="eval \`ssh-agent\`;ssh-add ~/.ssh/dl_key; rm -r ~/suq-controller; git clone git@gitlab.lrz.de:vadere/suq-controller.git; cd ~/suq-controller; cd ~/suq-controller; git checkout remote; sh refresh.sh"
ssh -p 5022 dlehmberg@minimuc.cs.hm.edu "${SCRIPT}"

