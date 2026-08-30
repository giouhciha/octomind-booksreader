# PDFBox admite codecs opcionales que no se incluyen porque la extracción adaptada solo necesita texto.
# La vista original usa PdfRenderer de Android y mantiene las imágenes aun cuando un codec opcional falte.
-dontwarn com.gemalto.jp2.**
-dontwarn com.levigo.jbig2.**
