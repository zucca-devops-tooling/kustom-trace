package model;

import parser.ReferenceType;

public record ResourceReference(ReferenceType referenceType, GraphNode resource) {
}
