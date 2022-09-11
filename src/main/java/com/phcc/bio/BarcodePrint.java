package com.phcc.bio;


import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.image.*;
import org.apache.pdfbox.text.PDFTextStripper;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;

public class BarcodePrint {

	private static Path currentPath = Paths.get("PDF");
	static PDDocument document = null;
	static String pmValue = null;

	public static void main(String[] args) throws IOException {
		Set<String> files = getCurrentFolderFiles();
		float x = 250;
		float y = 700;
		for (String pdfFileName : files) {
			String fileAbsolutePath = (currentPath.toAbsolutePath().toString() + FileSystems.getDefault().getSeparator()
					+ pdfFileName);
			System.out.println(new File(fileAbsolutePath));
			document = PDDocument.load(new File(
					currentPath.toAbsolutePath().toString() + FileSystems.getDefault().getSeparator() + pdfFileName));
			int pageIndex =  1;			
			String pageText = "";
			for (PDPage page : document.getPages()) {				
				pageText = getPageText(pageIndex);
				getExtractPPMNo(pageText);
				System.out.println(pmValue);
				if (pmValue!= null)
				addBarcode128(document, page, pmValue, x, y);
				pageIndex++;
				if (pageIndex >= document.getNumberOfPages() )
					break;
			}
			document.save(fileAbsolutePath);
			document.close();
		}
	}

	private static void getExtractPPMNo(String text) {
		pmValue = null;
		List <String> lines = arrayToList( text.split(System.getProperty("line.separator")));
		lines.stream().filter(x -> x.startsWith("WO  Number")).forEach(x -> {
			pmValue = (x.replaceAll("\\s+$", "").split(" ")[4]);
		});
	}
	
	private static <T> List <T> arrayToList (T array[]){
		List<T> list = new ArrayList<>();
		for (T t : array)
		list.add(t);
		return list;   
	}

	private static Set<String> getCurrentFolderFiles() {
		Set<String> files = new HashSet<String>();
		try (Stream<Path> stream = Files.walk(currentPath, 1)) {
			files = stream.filter(file -> !Files.isDirectory(file)).map(Path::getFileName).map(Path::toString)
					.collect(Collectors.toSet());
		} catch (IOException e) {
			System.err.println("Please create PDF folder and files.");
			System.err.println("current path is " + currentPath.toAbsolutePath().toString());
		}

		if (files.isEmpty())
			System.err.println("Please add pdf files in PDF folder.");
		return files;
	}

//

	private static void addBarcode128(PDDocument document, PDPage page, String text, float x, float y) {

		try {
			PDPageContentStream contentStream = new PDPageContentStream(document, page,
					PDPageContentStream.AppendMode.APPEND, true);
			int dpi = 300;
			BitmapCanvasProvider canvas = new BitmapCanvasProvider(dpi, BufferedImage.TYPE_BYTE_BINARY, false, 0);
			Code128Bean code128Bean = new Code128Bean();
			code128Bean.generateBarcode(canvas, text.trim());
			canvas.finish();
			BufferedImage bImage = canvas.getBufferedImage();
			PDImageXObject image = JPEGFactory.createFromImage(document, bImage);
			contentStream.drawImage(image, x, y, 100, 50);
			contentStream.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String getPageText(int page) throws IOException {
		String result = "?";
		PDFTextStripper pdfStripper = new PDFTextStripper();
		pdfStripper.setStartPage(page);
		pdfStripper.setEndPage(page);
		result = pdfStripper.getText(document);
		return result;
	}
}