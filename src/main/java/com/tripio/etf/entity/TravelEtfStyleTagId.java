package com.tripio.etf.entity;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode
public class TravelEtfStyleTagId implements Serializable {

    private Long travelEtfId;
    private Long styleTagId;
}
