package com.example.hotspotmanager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;

import com.itextpdf.io.source.ByteArrayOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class PDFGenerator {
    private static final String TAG = "PDFGenerator";

    public static byte[] generatePDF(String date, Map<String, Boolean> attendanceData) {
        // Create a new document
        PdfDocument document = new PdfDocument();

        // Add a page to the document
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        // Create a Paint object for drawing text
        Paint paint = new Paint();
        paint.setTextSize(12);
        paint.setColor(Color.BLACK);

        // Draw the attendance data on the page
        Canvas canvas = page.getCanvas();
        int y = 25;
        canvas.drawText("Attendance for " + date, 10, y, paint);
        y += 15;

        for (Map.Entry<String, Boolean> entry : attendanceData.entrySet()) {
            canvas.drawText(entry.getKey() + ": " + (entry.getValue() ? "Present" : "Absent"), 10, y, paint);
            y += 15;
        }

        document.finishPage(page);

        // Save the document as a byte array
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            document.writeTo(byteArrayOutputStream);
        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF", e);
            return null;
        } finally {
            document.close();
        }

        return byteArrayOutputStream.toByteArray();
    }
}
