#!/bin/bash

FILES=(
  'build.gradle'
  'gradle.properties'
  'src/main/java/caeruleusTait/WorldGen/WorldGen.java'
)


cd "$(dirname "$(readlink -fn "$0")")"

cat << EOF

       WorldGen Mod loader switcher
       ============================

  Please select the desired Minecraft Mod loader:

    1: Fabric
    2: Forge

EOF

read -p 'Please enter the number of the desired mod loader: ' LOADER
read -p 'Use symlinks? [Y/n]: ' LINK

case $LOADER in
  1)
    echo -n "Switching to Fabric"
    ENDING='fabric'
    ;;
  2)
    echo -n "Switching to Forge"
    ENDING='forge'
    ;;
  *)
    echo "Invalid selection: $LOADER"
    exit 1
esac

[ -z "$LINK" ] && LINK=y

case $LINK in
  Y|y|Yes|YES|yes)
    echo " by linking"
    CP='ln -s'
    ;;
  N|n|No|NO|no)
    echo " by copying"
    CP=cp
    ;;
  *)
    echo "Unrecognized input: $LINK"
    exit 1
esac

for f in "${FILES[@]}"; do
  rm -f "$f"
  dest="$PWD/$f.$ENDING"
  [ ! -e "$dest" ] && touch "$dest"
  $CP "$dest" "$f"
done
