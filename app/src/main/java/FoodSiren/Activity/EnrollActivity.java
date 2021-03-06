package FoodSiren.Activity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import com.bumptech.glide.Glide;
import com.example.eml_listview_test3.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.Date;

import FoodSiren.DB.AppDatabase;
import FoodSiren.DB.FoodDao;
import FoodSiren.DB.FoodViewModel;
import FoodSiren.Data.Category;
import FoodSiren.Data.Food;
import FoodSiren.Widget.WidgetListview;

public class EnrollActivity extends AppCompatActivity implements View.OnClickListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_BARCODE_INFO = 2;

    private Spinner categorySpinner;
    private ArrayAdapter<Category> categoryAdapter;
    private String selectedCategory;

    private String mCurrentPhotoPath = null;

    private ImageButton btn_Camera;
    private ImageButton btn_Barcode;
    private ImageView iv_photo;
    private EditText et_barcode;
    private EditText et_foodName;
    private Date regDate;
    private Date expDate;
    private Button btn_Add;
    private Button btn_Minus;
    private TextView tv_foodCount;
    private Button btn_Save;
    private int count = 1;
    private EditText et_reg_date, et_exp_date;
    private DatePickerDialog.OnDateSetListener callbackMethodRegCalBtn;
    private DatePickerDialog.OnDateSetListener callbackMethodExpCalBtn;

    private ViewModelProvider.AndroidViewModelFactory viewModelFactory;
    private ViewModelStore viewModelStore = new ViewModelStore();
    private FoodViewModel viewModel;

    private IntentIntegrator qrScan;
    private TextView DBTestTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll);

        if(viewModelFactory == null) {
            viewModelFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication());
        }
        viewModel = new ViewModelProvider(this, viewModelFactory).get(FoodViewModel.class);

        DBTestTv = findViewById(R.id.db_test_tv);

        // ????????? ???????????? getAll??? observe???????????? ????????????, ?????? getAll??? ????????? ?????????(=???????????? ????????????) ?????? ????????? ?????? ??????
        // ???????????? ??????. ?????? ?????? ????????? ???????????? ??????
        viewModel.getAllUpdatedFoods().observe(this, foods -> {  // foods ??? DB??? ???????????? food ????????? ??????
            DBTestTv.setText(foods.toString());  // Enroll ???????????? ?????? ??????
//            foodList.clear();  // foodList??? ?????????
//            foodList.addAll(foods);  // foodList??? ????????? ???????????? ??????
        });

        InitializeView();
        EditTextWatcher();
        InitializeListener();
        SetRecyclerAdapter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModelStore.clear();
    }

    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }

    private void InitializeView() {
        btn_Camera = (ImageButton) findViewById(R.id.EnrollActivity_btn_camera);
        btn_Barcode = (ImageButton) findViewById(R.id.EnrollActivity_btn_barcode);
        btn_Add = findViewById(R.id.EnrollActivity_btn_add);
        btn_Minus = findViewById(R.id.EnrollActivity_btn_minus);
        btn_Save = findViewById(R.id.EnrollActivity_btn_save);

        iv_photo = findViewById(R.id.EnrollActivity_iv_photo);
        et_barcode = (EditText) findViewById(R.id.EnrollActivity_et_bar_code);
        et_foodName = (EditText) findViewById(R.id.EnrollActivity_et_food_name);
        et_reg_date = (EditText) findViewById(R.id.EnrollActivity_et_reg_date);
        et_exp_date = (EditText) findViewById(R.id.EnrollActivity_et_exp_date);
        tv_foodCount = findViewById(R.id.EnrollActivity_tv_count);
        tv_foodCount.setText(count + "");

        btn_Add.setOnClickListener(this);
        btn_Save.setOnClickListener(this);
        btn_Camera.setOnClickListener(this);
        btn_Minus.setOnClickListener(this);
        btn_Barcode.setOnClickListener(this);
        qrScan = new IntentIntegrator(this);
    }

    private void EditTextWatcher() {
        et_reg_date.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String regDate = et_reg_date.getText().toString();
                if (regDate.length() == 8) {
                    StringToDate(et_reg_date.getText().toString(), et_reg_date);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        et_exp_date.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String expDate = et_exp_date.getText().toString();
                if (expDate.length() == 8) {
                    StringToDate(et_exp_date.getText().toString(), et_exp_date);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }


    private void InitializeListener() {
        callbackMethodRegCalBtn = (view, year, monthOfYear, dayOfMonth) -> {
            int intMonthOfYear = monthOfYear + 1; // monthOfYear??? ????????? 0~11 ????????? +1
            et_reg_date.setText(year + "???" + intMonthOfYear + "???" + dayOfMonth + "???");
        };
        callbackMethodExpCalBtn = (view, year, monthOfYear, dayOfMonth) -> {
            int intMonthOfYear = monthOfYear + 1; // monthOfYear??? ????????? 0~11 ????????? +1
            et_exp_date.setText(year + "???" + intMonthOfYear + "???" + dayOfMonth + "???");
        };
    }


    private void SetRecyclerAdapter() {
        categoryAdapter = new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_spinner_dropdown_item,
                SplashActivity.categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        categorySpinner = (Spinner) findViewById(R.id.EnrollActivity_category_spinner_enroll);

        categorySpinner.setAdapter(categoryAdapter);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedCategory = categorySpinner.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void StringToDate(String date, EditText et) {
        try {
            SimpleDateFormat fromFormat = new SimpleDateFormat("yyyyMMdd");
            Date objDate = fromFormat.parse(date);

            SimpleDateFormat toFormat = new SimpleDateFormat("yyyy???MM???dd???");
            String resultDate = toFormat.format(objDate);

            et.setText(resultDate);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String DateToString(String parseDate) {
        String splitRegByYear[] = parseDate.split("???");
        String regYear = splitRegByYear[0];
        String regMonthAndDayOfMonth = splitRegByYear[1];

        String splitRegByMonth[] = regMonthAndDayOfMonth.split("???");
        String regMonth = splitRegByMonth[0];
        String regDayOfMonth = splitRegByMonth[1].replace("???", "");

        System.out.println("??? : " + regYear);
        System.out.println("??? : " + regMonth);
        System.out.println("??? : " + regDayOfMonth);

        if (parseDate.length() != 11) {
            if (Integer.parseInt(regMonth) < 10)
                regMonth = "0" + regMonth;

            if (Integer.parseInt(regDayOfMonth) < 10)
                regDayOfMonth = "0" + regDayOfMonth;
        }

        return regYear + regMonth + regDayOfMonth;
    }

    private void ShelfLifeSave() {
        if (et_reg_date.getText().toString().equals("") || et_exp_date.getText().toString().equals("")) {
            Toast.makeText(getApplicationContext(), "????????? ???????????????!", Toast.LENGTH_SHORT).show();
        } else {
            String strfoodName = et_foodName.getText().toString();

            // ???????????? ????????? ?????? ????????? ??????
            String regDate = et_reg_date.getText().toString();
            if (regDate.length() > 8) {
                regDate = DateToString(regDate);
            }

            // ???????????? ????????? ?????? ????????? ??????
            String expDate = et_exp_date.getText().toString();
            if (expDate.length() > 8) {
                expDate = DateToString(expDate);
            }

            try {
                SimpleDateFormat fromFormat = new SimpleDateFormat("yyyyMMdd");
                Date objDate1 = fromFormat.parse(regDate);
                Date objDate2 = fromFormat.parse(expDate);

                String limitReg = regDate.charAt(0) + "" + regDate.charAt(1) + "" + regDate.charAt(2) + "" + regDate.charAt(3);
                String limitExp = expDate.charAt(0) + "" + expDate.charAt(1) + "" + expDate.charAt(2) + "" + expDate.charAt(3);

                if (regDate.length() == 8 && expDate.length() == 8) {
                    if (Integer.parseInt(limitReg) < 2100 && Integer.parseInt(limitReg) > 2020
                            && Integer.parseInt(limitExp) < 2100 && Integer.parseInt(limitExp) > 2020) {

                        Food newFood;
                        if (mCurrentPhotoPath != null) {
                            newFood = new Food(mCurrentPhotoPath, strfoodName, regDate, expDate, selectedCategory);
                        } else {
                            newFood = new Food(null, strfoodName, regDate, expDate, selectedCategory);
                        }
                        // ?????? ??????
//                        db.foodDao().insert(newFood);  // DB??? ?????? ????????? ???????????? ?????? (Activity?????? db??? ?????? ???????????? ????????? ?????????.)
                        viewModel.insert(newFood);
//                        foodList.add(newFood);
//                        SplashActivity.sortingFoodData();
                        System.out.println("DB??? ?????? ?????? ??????");
                        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(
                                new ComponentName(this, WidgetListview.class));
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_listview); // ?????? ????????????
                    } else {
                        Toast.makeText(getApplicationContext(), "????????? ????????? ?????????.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "????????? ????????? ?????????.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "????????? ?????? ????????????.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.EnrollActivity_btn_save:
                ShelfLifeSave();
                break;

            case R.id.EnrollActivity_btn_camera:
                getFoodImage();
                break;

            case R.id.EnrollActivity_btn_add:
                count++;
                tv_foodCount.setText(count + "");
                break;

            case R.id.EnrollActivity_btn_minus:
                if (count > 0) {
                    count--;
                    tv_foodCount.setText(count + "");
                }
                break;

            case R.id.EnrollActivity_btn_barcode:
                startbarcode();
                break;
        }
    }

    public void OnClickHandlerRegCalBtn(View view) {
        Date currentDate = new Date();
        DatePickerDialog dialog = new DatePickerDialog(this, callbackMethodRegCalBtn, currentDate.getYear() + 1900, currentDate.getMonth(), currentDate.getDate());

        dialog.show();
    }

    public void OnClickHandlerExpCalBtn(View view) {
        Date currentDate = new Date();
        DatePickerDialog dialog = new DatePickerDialog(this, callbackMethodExpCalBtn, currentDate.getYear() + 1900, currentDate.getMonth(), currentDate.getDate());

        dialog.show();
    }

    public void getFoodImage() {
        Intent intent = new Intent(getApplicationContext(), PhotoActivity.class);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    public void startbarcode() {
        qrScan.setPrompt("Scanning...");
        qrScan.initiateScan();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                mCurrentPhotoPath = data.getStringExtra("result");
                Glide.with(this).load(mCurrentPhotoPath).into(iv_photo);
            }
        } else if (requestCode == REQUEST_BARCODE_INFO) {
            if (resultCode == RESULT_OK) {
                Intent LoadingIntent = getIntent();
                String FoodBarcodeName = data.getStringExtra("?????????_??????_??????");
                et_foodName.setText(FoodBarcodeName);
                mCurrentPhotoPath = data.getStringExtra("?????????_??????_??????");
                Glide.with(this).load(mCurrentPhotoPath).into(iv_photo);
            }
        }
        //zxing??? ????????? ????????? ????????? ????????? json ??????????????? ???????????? ?????????????????? ????????? onActivityResult?????? ????????? ?????? ????????? ??????????????? ???.
        else if (result != null) {
            //qrcode ??? ?????????
            if (result.getContents() == null) {
                //Toast.makeText(EnrollActivity.this, "??????!", Toast.LENGTH_SHORT).show();
            } else {
                //qrcode ????????? ?????????
                et_barcode.setText(result.getContents());
                Intent intent = new Intent(getApplicationContext(), LoadingActivity.class);
                intent.putExtra("?????????_??????", result.getContents());
                startActivityForResult(intent, REQUEST_BARCODE_INFO);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

