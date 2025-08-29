package com.pppk.patients.patients_domain.port;

import com.pppk.patients.patients_domain.vo.Oib;

public interface OibUniquenessChecker { boolean exists(Oib oib); }