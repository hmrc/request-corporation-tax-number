<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">

  <xsl:attribute-set name="print-document">
    <xsl:attribute name="font-family">GDS Transport,arial,sans-serif</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="govuk-heading-l">
    <xsl:attribute name="color">#0b0c0c</xsl:attribute>
    <xsl:attribute name="font-weight">700</xsl:attribute>
    <xsl:attribute name="font-size">1.5em</xsl:attribute>
    <xsl:attribute name="line-height">1.04167</xsl:attribute>
    <xsl:attribute name="margin-top">0px</xsl:attribute>
    <xsl:attribute name="margin-bottom">20px</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="heading-large">
    <xsl:attribute name="color">#6f777b</xsl:attribute>
    <xsl:attribute name="margin-bottom">0.4em</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="heading">
    <xsl:attribute name="font-size">1.5em</xsl:attribute>
    <xsl:attribute name="margin-top">1em</xsl:attribute>
    <xsl:attribute name="margin-bottom">0.4em</xsl:attribute>
    <!-- All added for improved PDF layout -->
  </xsl:attribute-set>

  <xsl:attribute-set name="govuk-grid-column-full">
    <xsl:attribute name="padding-top">0px</xsl:attribute>
    <xsl:attribute name="padding-bottom">0px</xsl:attribute>
    <xsl:attribute name="padding-left">15px</xsl:attribute>
    <xsl:attribute name="padding-right">15px</xsl:attribute>
    <!-- Removed unsupported properties: box-sizing, width-->
  </xsl:attribute-set>

  <xsl:attribute-set name="govuk-grid-row">
    <!-- Removed margin-* -->
  </xsl:attribute-set>

  <xsl:attribute-set name="govuk-heading-m">
    <xsl:attribute name="color">#0b0c0c</xsl:attribute>
    <xsl:attribute name="font-family">GDS Transport,Helvetica,sans-serif</xsl:attribute>
    <xsl:attribute name="font-weight">700</xsl:attribute>
    <xsl:attribute name="font-size">1.125em</xsl:attribute>
    <xsl:attribute name="line-height">1.11111</xsl:attribute>
    <xsl:attribute name="margin-top">0px</xsl:attribute>
    <xsl:attribute name="margin-bottom">15px</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="govuk-list">
    <xsl:attribute name="color">#0b0c0c</xsl:attribute>
    <xsl:attribute name="font-family">GDS Transport,Helvetica,sans-serif</xsl:attribute>
    <xsl:attribute name="font-weight">400</xsl:attribute>
    <xsl:attribute name="font-size">1em</xsl:attribute>
    <xsl:attribute name="line-height">1.25</xsl:attribute>
    <xsl:attribute name="margin-top">0px</xsl:attribute>
    <xsl:attribute name="margin-bottom">20px</xsl:attribute>
    <xsl:attribute name="table-layout">fixed</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="govuk-list__row">
    <xsl:attribute name="border-bottom">1px solid #b1b4b6</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="govuk-list__key">
    <xsl:attribute name="padding-top">10px</xsl:attribute>
    <xsl:attribute name="padding-right">20px</xsl:attribute>
    <xsl:attribute name="padding-bottom">10px</xsl:attribute>
    <xsl:attribute name="font-family">GDS Transport,Helvetica,sans-serif</xsl:attribute>
    <xsl:attribute name="font-weight">700</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="govuk-list__value">
    <xsl:attribute name="padding-top">10px</xsl:attribute>
    <xsl:attribute name="padding-right">20px</xsl:attribute>
    <xsl:attribute name="padding-bottom">10px</xsl:attribute>
  </xsl:attribute-set>

</xsl:stylesheet>
