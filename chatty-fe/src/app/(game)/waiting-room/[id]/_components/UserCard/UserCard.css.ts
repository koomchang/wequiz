import { style } from '@vanilla-extract/css';
import { recipe } from '@vanilla-extract/recipes';
import { globals } from '@/app/globals.css';

export const cardContainer = style({
  display: 'flex',
  height: '100%',
  width: '100%',
  padding: '8px 0 16px 0',
  flexDirection: 'column',
  justifyContent: 'flex-end',
  alignItems: 'center',
  gap: 16,
  backgroundColor: globals.color.blue_5,
  borderRadius: 20,
  border: '2px solid #fff',
  position: 'relative',
  // overflow: 'hidden',
  boxShadow: '0 0 10px rgba(0, 0, 0, 0.1)',
  '@media': {
    '(max-width: 768px)': {
      padding: '0 0 12px 0',
    },
  },
});

export const readyContainer = style({
  boxShadow: '0 0 18px 0 #2692FF',
});

export const avatar = style({
  position: 'relative',
  display: 'flex',
  width: '100%',
  height: 'calc(100% - 63px)',
  flexDirection: 'column',
  gap: 10,
});

export const chatContainer = recipe({
  base: {
    position: 'absolute',
    top: -25,
    display: 'flex',
    flexDirection: 'column',
    gap: 0,
    width: '100%',
    alignItems: 'center',
    opacity: 1,
    transition: 'opacity 0.2s ease',
    zIndex: 5,
  },
  variants: {
    fadeOut: {
      true: {
        opacity: 0,
      },
    },
  },
});

export const chatMessage = style({
  position: 'relative',
  maxWidth: '80%',
  wordWrap: 'break-word',
  backgroundColor: 'white',
  borderRadius: 12,
  color: globals.color.black,
  fontSize: 14,
  fontWeight: 400,
  textAlign: 'center',
  padding: '6px 12px',
  '::after': {
    content: '""',
    position: 'absolute',
    bottom: '-7px',
    left: '50%',
    transform: 'translateX(-50%)',
    borderStyle: 'solid',
    borderWidth: '10px 10px 0',
    borderColor: 'white transparent',
    zIndex: 5,
  },
  "@media" : {
    "(max-width: 768px)": {
      fontSize: 10,
    },
  }
});

export const profileImage = style({
  display: 'flex',
  position: 'relative',
  top: "5%",
  justifyContent: 'center',
  '@media': {
    '(max-width: 768px)': {
      // top: '0',
    },
  },
});

export const userInfo = style({
  width: '100%',
  zIndex: 6,
  overflow: 'hidden',
});

export const line = style({
  width: '100%',
  height: 2,
  background: `linear-gradient(270deg, ${globals.color.blue_4} 0%, #FFF 49.5%, ${globals.color.blue_4} 100%)`,
});

export const userName = style({
  display: 'flex',
  flexWrap: 'wrap',
  width: '100%',
  padding: '8px 0',
  color: globals.color.black,
  backgroundColor: globals.color.blue_4,
  fontSize: 24,
  fontWeight: 600,
  justifyContent: 'center',
  alignContent: 'center',
  overflow: 'hidden',
  '@media': {
    '(max-width: 768px)': {
      padding: '4px 0',
      fontSize: '3vw',

    },
  },
});
