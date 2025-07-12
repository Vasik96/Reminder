package com.reminder.ui.notes;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.reminder.R;
import com.reminder.databinding.FragmentNotesBinding;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public class NotesFragment extends Fragment {

    private FragmentNotesBinding binding;
    private Context context;

    private static final String NOTES_FILE = "notes.txt";
    TextInputLayout text_input_layout;
    TextInputEditText text_field;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentNotesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        context = getContext();

        MaterialToolbar toolbar = binding.toolbar;
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_save) {
                    saveFile(Objects.requireNonNull(text_input_layout.getEditText()).getText().toString());
                    return true;
                }
                return false;
            }
        });

        text_field = binding.notesTextfieldedittext;
        text_input_layout = binding.notesTextfield;

        loadFileAndSetText();

        return root;
    }

    private void saveFile(String notes) {
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = context.openFileOutput(NOTES_FILE, MODE_PRIVATE);
            fileOutputStream.write(notes.getBytes());
            Toast.makeText(context, context.getString(R.string.save_success), Toast.LENGTH_SHORT).show();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
    private void loadFileAndSetText() {
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = context.openFileInput(NOTES_FILE);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String text_from_file;

            while ((text_from_file = bufferedReader.readLine()) != null) {
                stringBuilder.append(text_from_file).append("\n");
            }
            text_from_file = stringBuilder.toString();
            text_field.setText(text_from_file);


        } catch (FileNotFoundException e) {
            text_field.setText("");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
