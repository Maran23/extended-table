# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Prerequisite

- I'm a Java and JavaFX expert and contributor
- This is a high quality, following usual / best practice of table frameworks (such as AG-Grid)
- Table API and CSS should be the same as the Tree Table API (except Tree lines) 

## Styling

- The stylesheets use only the looked-up colors of Modena, so they work with Modena and with ShadFX, which sets them all
- Tree lines are `-fx-text-background-color`, not `-fx-box-border`: it is `-border` in ShadFX, too faint for the lines
