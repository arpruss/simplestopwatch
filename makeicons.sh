for x in settings menu; do
    convert $x.png -resize 96x96 app/src/main/res/drawable-xxhdpi/$x.png
    convert $x.png -resize 64x64 app/src/main/res/drawable-xhdpi/$x.png
    convert $x.png -resize 48x48 app/src/main/res/drawable-hdpi/$x.png
    convert $x.png -resize 32x32 app/src/main/res/drawable-mdpi/$x.png
done