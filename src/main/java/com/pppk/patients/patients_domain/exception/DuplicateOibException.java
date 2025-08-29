package com.pppk.patients.patients_domain.exception;

public class DuplicateOibException extends RuntimeException { public DuplicateOibException(String oib){super("OIB exists: "+oib);} }
