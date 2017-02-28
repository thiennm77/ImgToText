package com.thiennm77.imgtotext;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

public class PhotoActivity extends Activity implements View.OnClickListener {

    private ImageView photo;
    private TextView result;
    private Button copyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        photo = (ImageView) findViewById(R.id.photo);
        result = (TextView) findViewById(R.id.result);
        copyButton = (Button) findViewById(R.id.copy);
        copyButton.setOnClickListener(this);

        Bitmap data = (Bitmap) getIntent().getExtras().get("bitmap");

        if (data == null) {
            Uri uri = (Uri) getIntent().getExtras().get("uri");
            if (uri == null) {
                result.setText("Something is wrong!!!");
                result.setTextColor(Color.RED);
                return;
            }
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            data = decodeSampledBitmapFromFile(getPath(uri), size.x, size.x);
        }
        photo.setAdjustViewBounds(true);
        photo.setImageBitmap(data);

        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
        if (!textRecognizer.isOperational()) {
            Log.w("ImgToText", "PhotoActivity -> Detector dependencies are not yet available.");

            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this,"Low Storage", Toast.LENGTH_LONG).show();
                Log.w("ImgToText", "Low Storage");
            }
        }

        Frame imageFrame = new Frame.Builder().setBitmap(data).build();
        SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);
        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));

            Log.i("ImgToText", textBlock.getValue());
            result.setText(result.getText() + " " + textBlock.getValue());
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.copy) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("ImgToTextResult", result.getText());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT);
        }
    }

    private String getPath(Uri uri) {
        // just some safety built in
        if( uri == null ) {
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        // this is our fallback here
        return uri.getPath();
    }

    private int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }
}
