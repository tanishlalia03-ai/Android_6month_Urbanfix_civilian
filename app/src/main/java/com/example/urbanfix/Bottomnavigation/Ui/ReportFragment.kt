package com.example.urbanfix.Bottomnavigation.Ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.urbanfix.databinding.FragmentReportBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    // Class-level variables to store the data silently
    private var selectedDate: String = ""
    private var selectedTime: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // DATE PICKER LOGIC
        binding.btnPickDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.show(parentFragmentManager, "DATE_PICKER")

            datePicker.addOnPositiveButtonClickListener { select ->
                // Store date in variable
                selectedDate = SimpleDateFormat("dd/MMM/yyyy", Locale.getDefault()).format(Date(select))

                updateDisplay()
            }
        }

        // TIME PICKER LOGIC
        binding.btnPickTime.setOnClickListener {
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Time")
                .build()

            timePicker.show(parentFragmentManager, "TIME_PICKER")

            timePicker.addOnPositiveButtonClickListener {
                val hour = timePicker.hour
                val minute = timePicker.minute
                // Store time in variable
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                updateDisplay()
            }
        }
    } // End of onViewCreated


    // Simple function to combine the strings and show them in the box
    private fun updateDisplay() {
        val combinedText = if (selectedDate.isNotEmpty() && selectedTime.isNotEmpty()) {
            "$selectedDate - $selectedTime"
        } else {
            selectedDate + selectedTime // Shows whichever one is not empty
        }

        binding.btnPickDate.setText(combinedText)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}