cp mipmap-xxxhdpi/*.png mipmap-xxhdpi/
cp mipmap-xxxhdpi/*.png mipmap-xhdpi/
cp mipmap-xxxhdpi/*.png mipmap-hdpi/
cp mipmap-xxxhdpi/*.png mipmap-mdpi/

mogrify -resize 192x192 mipmap-xxxhdpi/*.png
mogrify -resize 144x144 mipmap-xxhdpi/*.png
mogrify -resize 96x96 mipmap-xhdpi/*.png
mogrify -resize 72x72 mipmap-hdpi/*.png
mogrify -resize 48x48 mipmap-mdpi/*.png

