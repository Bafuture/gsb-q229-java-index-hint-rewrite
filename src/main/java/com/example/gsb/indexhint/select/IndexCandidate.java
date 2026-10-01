package com.example.gsb.indexhint.select;

import com.example.gsb.indexhint.model.IndexDefinition;

public record IndexCandidate(IndexDefinition index, boolean usable, String reason,
                             double filterRatio, double cost) {
}
