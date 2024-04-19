import { style } from '@vanilla-extract/css';
import { globals } from '../../../../globals.css';

export const gameListContainer = style({
  display: 'flex',
  flexDirection: 'column',
  width: '100%',
  height: 'calc(100% - 90px)',
  padding: 24,
  alignItems: 'center',
  backgroundColor: globals.color.blue_main,
  borderRadius: 26,
  position: 'relative',
});

export const gameListGrid = style({
  display: 'grid',
  gap: '24px',
  width: '100%',
  height: 'calc(100%-48px)',
  overflowY: 'scroll',
  gridTemplateColumns:
    'repeat(auto-fill, minmax(calc((100% - 48px - 24px) / 2), 1fr))',
  '@media': {
    '(max-width: 554px)': {
      gridTemplateColumns: 'repeat(auto-fill, minmax(calc(100% - 48px), 1fr))',
    },
  },
  paddingTop: 4,
  paddingBottom: 4,
  paddingRight: 24,
  marginRight: -4,
  '::-webkit-scrollbar': {
    width: 24,
  },
  '::-webkit-scrollbar-track': {
    position: 'absolute',
    right: 0,
    boxShadow: '0px 0px 4px 0px rgba(17, 41, 66, 0.25) inset',
    borderRadius: 12,
    borderLeft: `2px solid ${globals.color.blue_main}`,
    borderRight: `2px solid ${globals.color.blue_main}`,
    background: 'transparent',
    height: 'calc(100% - 48px)',
    zIndex: 1,
  },
  '::-webkit-scrollbar-thumb': {
    borderLeft: '12px solid #fff',
    borderRight: '12px solid #fff',
    backgroundColor: globals.color.blue_7,
    borderRadius: 12,
    boxShadow: '0px 0px 4px 2px rgba(23, 96, 171, 0.25)',
    zIndex: 2,
  },
});