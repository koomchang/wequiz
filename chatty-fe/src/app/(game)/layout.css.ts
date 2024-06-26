import { style } from '@vanilla-extract/css';

export const mainContainer = style({
  padding: '50px 20px',
  display: 'flex',
  width: '100%',
  height: 'calc(100% - 60px)',
  justifyContent: 'center',
  '@media': {
    '(max-width: 768px)': {
      padding: 20,
    },
  },
  overflowY: 'scroll',
});
