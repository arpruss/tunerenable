mkdir res/drawable-xxhdpi
mkdir res/drawable-xhdpi
mkdir res/drawable-hdpi
mkdir res/drawable-mdpi

convert myicon.png -resize 144x144 res/drawable-xxhdpi/icon.png
convert myicon.png -resize 96x96 res/drawable-xhdpi/icon.png
convert myicon.png -resize 72x72 res/drawable-hdpi/icon.png
convert myicon.png -resize 48x48 res/drawable-mdpi/icon.png
convert myicon.png -resize 144x144 myicon144.png
convert myicon.png -resize 512x512 myicon512.png
