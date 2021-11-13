git push
ssh -i ~/.ssh/id_rsa mod@206.189.36.39
cd ~/server/prod/scripts/ || return
sh ./build.sh