package com.pppk.patients.patients_domain.exception;

public class PatientNotFoundException extends RuntimeException { public PatientNotFoundException(Long id){super("Patient "+id+" not found");} }
