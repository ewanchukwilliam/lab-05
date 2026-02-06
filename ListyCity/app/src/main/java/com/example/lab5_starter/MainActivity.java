package com.example.lab5_starter;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private static final String TAG = "MainActivity";
    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // Load cities from Firestore
        loadCities();

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
        });

        // Long press to delete
        cityListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            new AlertDialog.Builder(this)
                    .setTitle("Delete City")
                    .setMessage("Delete " + city.getName() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteCity(city))
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    private void loadCities() {
        citiesRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            cityArrayList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String name = doc.getString("name");
                String province = doc.getString("province");
                cityArrayList.add(new City(name, province));
            }
            cityArrayAdapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading cities", e));
    }

    @Override
    public void updateCity(City city, String newName, String newProvince) {
        String oldName = city.getName();
        city.setName(newName);
        city.setProvince(newProvince);
        cityArrayAdapter.notifyDataSetChanged();

        // Update Firestore: delete old doc, add new one
        citiesRef.document(oldName).delete();
        HashMap<String, String> data = new HashMap<>();
        data.put("name", newName);
        data.put("province", newProvince);
        citiesRef.document(newName).set(data);
    }

    @Override
    public void addCity(City city) {
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();

        // Add to Firestore
        HashMap<String, String> data = new HashMap<>();
        data.put("name", city.getName());
        data.put("province", city.getProvince());
        citiesRef.document(city.getName()).set(data);
    }

    @Override
    public void deleteCity(City city) {
        cityArrayList.remove(city);
        cityArrayAdapter.notifyDataSetChanged();

        // Delete from Firestore
        citiesRef.document(city.getName()).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "City deleted"))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting city", e));
    }
}
