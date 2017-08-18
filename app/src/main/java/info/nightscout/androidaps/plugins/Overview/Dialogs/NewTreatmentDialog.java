package info.nightscout.androidaps.plugins.Overview.Dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import java.text.DecimalFormat;

import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.interfaces.PumpInterface;
import info.nightscout.utils.PlusMinusEditText;
import info.nightscout.utils.SafeParse;

public class NewTreatmentDialog extends DialogFragment implements OnClickListener {

    Button okButton;
    Button cancelButton;
    TextView insulin;
    TextView carbs;

    PlusMinusEditText editCarbs;
    PlusMinusEditText editInsulin;

    Handler mHandler;
    public static HandlerThread mHandlerThread;

    public NewTreatmentDialog() {
        mHandlerThread = new HandlerThread(NewTreatmentDialog.class.getSimpleName());
        mHandlerThread.start();
        this.mHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.overview_newtreatment_dialog, null, false);

        okButton = (Button) view.findViewById(R.id.ok);
        okButton.setOnClickListener(this);
        cancelButton = (Button) view.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(this);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        insulin = (TextView) view.findViewById(R.id.treatments_newtreatment_insulinamount);
        carbs = (TextView) view.findViewById(R.id.treatments_newtreatment_carbsamount);

        Integer maxCarbs = MainApp.getConfigBuilder().applyCarbsConstraints(Constants.carbsOnlyForCheckLimit);
        Double maxInsulin = MainApp.getConfigBuilder().applyBolusConstraints(Constants.bolusOnlyForCheckLimit);

        editCarbs = new PlusMinusEditText(view, R.id.treatments_newtreatment_carbsamount, R.id.treatments_newtreatment_carbsamount_plus, R.id.treatments_newtreatment_carbsamount_minus, 0d, 0d, (double) maxCarbs, 1d, new DecimalFormat("0"), false);
        editInsulin = new PlusMinusEditText(view, R.id.treatments_newtreatment_insulinamount, R.id.treatments_newtreatment_insulinamount_plus, R.id.treatments_newtreatment_insulinamount_minus, 0d, 0d, maxInsulin, MainApp.getConfigBuilder().getPumpDescription().bolusStep, new DecimalFormat("0.00"), false);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getDialog() != null)
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ok:

                try {
                    Double insulin = SafeParse.stringToDouble(this.insulin.getText().toString());
                    Integer carbs = SafeParse.stringToInt(this.carbs.getText().toString());

                    String confirmMessage = getString(R.string.entertreatmentquestion) + "\n";

                    Double insulinAfterConstraints = MainApp.getConfigBuilder().applyBolusConstraints(insulin);
                    Integer carbsAfterConstraints = MainApp.getConfigBuilder().applyCarbsConstraints(carbs);

                    confirmMessage += getString(R.string.bolus) + ": " + insulinAfterConstraints + "U";
                    confirmMessage += "\n" + getString(R.string.carbs) + ": " + carbsAfterConstraints + "g";
                    if (insulinAfterConstraints - insulin != 0 || carbsAfterConstraints != carbs)
                        confirmMessage += "\n" + getString(R.string.constraintapllied);

                    final Double finalInsulinAfterConstraints = insulinAfterConstraints;
                    final Integer finalCarbsAfterConstraints = carbsAfterConstraints;

                    final Context context = getContext();
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);

                    builder.setTitle(this.getContext().getString(R.string.confirmation));
                    builder.setMessage(confirmMessage);
                    builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (finalInsulinAfterConstraints > 0 || finalCarbsAfterConstraints > 0) {
                                final PumpInterface pump = MainApp.getConfigBuilder();
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        PumpEnactResult result = pump.deliverTreatment(MainApp.getConfigBuilder().getActiveInsulin(), finalInsulinAfterConstraints, finalCarbsAfterConstraints, context);
                                        if (!result.success) {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                            builder.setTitle(MainApp.sResources.getString(R.string.treatmentdeliveryerror));
                                            builder.setMessage(result.comment);
                                            builder.setPositiveButton(MainApp.sResources.getString(R.string.ok), null);
                                            builder.show();
                                        }
                                    }
                                });
                                Answers.getInstance().logCustom(new CustomEvent("Bolus"));
                            }
                        }
                    });
                    builder.setNegativeButton(getString(R.string.cancel), null);
                    builder.show();
                    dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.cancel:
                dismiss();
                break;
        }

    }

}