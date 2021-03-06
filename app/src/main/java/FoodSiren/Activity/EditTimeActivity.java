package FoodSiren.Activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import com.example.eml_listview_test3.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import FoodSiren.DB.AppDatabase;
import FoodSiren.DB.FoodDao;
import FoodSiren.DB.FoodViewModel;
import FoodSiren.Receiver.AlarmReceiver;
import FoodSiren.Data.Food;

import static FoodSiren.Activity.SplashActivity.date_text;

public class EditTimeActivity extends AppCompatActivity {

    private AlarmManager alarmManager;
    public ArrayList<Food> foodList;

    private ViewModelProvider.AndroidViewModelFactory viewModelFactory;
    private ViewModelStore viewModelStore = new ViewModelStore();
    private FoodViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_time);

        if (viewModelFactory == null) {
            viewModelFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication());
        }
        viewModel = new ViewModelProvider(this, viewModelFactory).get(FoodViewModel.class);


        try {
            foodList = (ArrayList<Food>) viewModel.getAll();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final TimePicker picker = (TimePicker) findViewById(R.id.timePicker);
        picker.setIs24HourView(true);


        // ?????? ????????? ????????? ????????????
        // ????????? ????????? ?????? ????????????
        SharedPreferences sharedPreferences = getSharedPreferences("daily alarm", MODE_PRIVATE);
        long millis = sharedPreferences.getLong("nextNotifyTime", Calendar.getInstance().getTimeInMillis());

        Calendar nextNotifyTime = new GregorianCalendar();
        nextNotifyTime.setTimeInMillis(millis);

        Date nextDate = nextNotifyTime.getTime();


        // ?????? ??????????????? TimePicker ?????????
        Date currentTime = nextNotifyTime.getTime();
        SimpleDateFormat HourFormat = new SimpleDateFormat("kk", Locale.getDefault());
        SimpleDateFormat MinuteFormat = new SimpleDateFormat("mm", Locale.getDefault());

        int pre_hour = Integer.parseInt(HourFormat.format(currentTime));
        int pre_minute = Integer.parseInt(MinuteFormat.format(currentTime));


        if (Build.VERSION.SDK_INT >= 23) {
            picker.setHour(pre_hour);
            picker.setMinute(pre_minute);
        } else {
            picker.setCurrentHour(pre_hour);
            picker.setCurrentMinute(pre_minute);
        }


        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                int hour, hour_24, minute;
                String am_pm;
                if (Build.VERSION.SDK_INT >= 23) {
                    hour_24 = picker.getHour();
                    minute = picker.getMinute();
                } else {
                    hour_24 = picker.getCurrentHour();
                    minute = picker.getCurrentMinute();
                }


                // ?????? ????????? ???????????? ?????? ?????? ??????
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, hour_24);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);

                // ?????? ?????? ????????? ??????????????? ????????? ?????? ???????????? ??????
                if (calendar.before(Calendar.getInstance())) {
                    calendar.add(Calendar.DATE, 1);
                }

                Date currentDateTime = calendar.getTime();
                date_text = new SimpleDateFormat("kk:mm:ss", Locale.getDefault()).format(currentDateTime);
                Toast.makeText(getApplicationContext(), date_text + "?????? ????????? ?????????????????????!", Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = getSharedPreferences("daily alarm", MODE_PRIVATE).edit();
                editor.putLong("nextNotifyTime", (long) calendar.getTimeInMillis());
                editor.apply();
                int position = 0;
                for (Food food : foodList) {
                    if (food.getSwitchBoolean())
                        setAlarm(food.getSwitchBoolean(), position++);
                }
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });
    }

    public void setAlarm(boolean isAlarmOn, int position) {

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        Date expDate = null;
        try {
            expDate = df.parse(foodList.get(position).getExpDate());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar expMinus7 = Calendar.getInstance();
        expMinus7.setTime(expDate);
        expMinus7.add(Calendar.DATE, -7);
        String strExpMinus7 = df.format(expMinus7.getTime());

        Calendar expMinus3 = Calendar.getInstance();
        expMinus3.setTime(expDate);
        expMinus3.add(Calendar.DATE, -3);
        String strExpMinus3 = df.format(expMinus3.getTime());

        Calendar expPlus3 = Calendar.getInstance();
        expPlus3.setTime(expDate);
        expPlus3.add(Calendar.DATE, +3);
        String strExpPlus3 = df.format(expPlus3.getTime());

        Calendar expPlus7 = Calendar.getInstance();
        expPlus7.setTime(expDate);
        expPlus7.add(Calendar.DATE, +7);
        String strExpPlus7 = df.format(expPlus7.getTime());

        int DoubleRequestCode = position * 5;

        try {
            if ((foodList.get(position).getDiffDaysCurrentToExp() + 1 >= 7) && (!foodList.get(position).isExpOver())) { // ?????????????????? ?????? ????????? 7??? ????????? ??????, D-7??? ?????? ??????
                makeNotification(isAlarmOn, position, DoubleRequestCode + 0, strExpMinus7);
                makeNotification(isAlarmOn, position, DoubleRequestCode + 1, strExpMinus3);
                makeNotification(isAlarmOn, position, DoubleRequestCode + 2, foodList.get(position).getExpDate());
                makeNotification(isAlarmOn, position, DoubleRequestCode + 3, strExpPlus3);
                makeNotification(isAlarmOn, position, DoubleRequestCode + 4, strExpPlus7);
            } else if ((foodList.get(position).getDiffDaysCurrentToExp() + 1 >= 3) && (!foodList.get(position).isExpOver())) { // ?????????????????? ?????? ????????? 3??? ????????? ??????, D-3??? ?????? ??????
                makeNotification(isAlarmOn, position, DoubleRequestCode + 1, strExpMinus3);
                makeNotification(isAlarmOn, position, DoubleRequestCode + 2, foodList.get(position).getExpDate());
                makeNotification(isAlarmOn, position, DoubleRequestCode + 3, strExpPlus3);
                makeNotification(isAlarmOn, position, DoubleRequestCode + 4, strExpPlus7);
            } else if ((foodList.get(position).isExpOver()) && (foodList.get(position).getDiffDaysCurrentToExp() == 0)) { // ??????????????? ?????? ???????????? ??????, 0??? ??????, D+3??? ??????, D+7??? ?????? ??????
                makeNotification(isAlarmOn, position, DoubleRequestCode + 2, foodList.get(position).getExpDate());
                makeNotification(isAlarmOn, position, DoubleRequestCode + 3, strExpPlus3);
                makeNotification(isAlarmOn, position, DoubleRequestCode + 4, strExpPlus7);
            } else if ((foodList.get(position).isExpOver()) && (foodList.get(position).getDiffDaysCurrentToExp() >= 1)) { // ??????????????? ?????? ??????, Toast ????????? ??????
                makeNotification(isAlarmOn, position, DoubleRequestCode + 3, strExpPlus3);
                makeNotification(isAlarmOn, position, DoubleRequestCode + 4, strExpPlus7);
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    private void makeNotification(boolean isAlarmOn, int position, int DoubleRequestCode, String strAlarmDate) {
        Intent receiverIntent = new Intent(getApplicationContext(), AlarmReceiver.class);
        Calendar getToday = Calendar.getInstance();
        getToday.setTime(new Date()); // ?????? ??????
        try {
            if ((foodList.get(position).isExpOver()) && (foodList.get(position).getDiffDaysCurrentToExp() == 0)) {
                receiverIntent.putExtra("?????? ??????", foodList.get(position).getDiffDaysCurrentToExp());
            } else if (foodList.get(position).isExpOver()) {
                receiverIntent.putExtra("?????? ??? ???", foodList.get(position).getDiffDaysCurrentToExp());
            } else {
                receiverIntent.putExtra("?????? ??? ???", foodList.get(position).getDiffDaysCurrentToExp() + 1);
            }
        } catch (
                ParseException e) {
            e.printStackTrace();
        }
        receiverIntent.putExtra("??????", foodList.get(position).getName());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), DoubleRequestCode, receiverIntent, PendingIntent.FLAG_CANCEL_CURRENT);


        String alarmTime = strAlarmDate + " " + date_text; //????????? ????????? ????????? ??????
        alarmManager = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        if (isAlarmOn == true) {
            //?????? ????????? ???????????? ????????????
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd kk:mm:ss");
            Date alarmDate = null;
            try {
                alarmDate = df.parse(alarmTime);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Calendar alarmThisTime = Calendar.getInstance();
            alarmThisTime.setTime(alarmDate);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    //API 19 ?????? API 23??????
                    alarmManager.setExact(AlarmManager.RTC, alarmThisTime.getTimeInMillis(), pendingIntent);
                } else {
                    //API 19??????
                    alarmManager.set(AlarmManager.RTC, alarmThisTime.getTimeInMillis(), pendingIntent);
                }
            } else {
                //API 23 ??????
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, alarmThisTime.getTimeInMillis(), pendingIntent);
            }
        } else
            alarmManager.cancel(pendingIntent);
    }
}
