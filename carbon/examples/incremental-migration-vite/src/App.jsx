import React from 'react';
import {
  Button,
  Content,
  Header,
  SkipToContent,
  HeaderName,
  Toggle,
  NumberInput,
  RadioButtonGroup,
  RadioButton,
  Search,
  Select,
  SelectItem,
  TextInput,
  TextArea,
} from 'carbon-components-react';
import {
  StructuredListBody,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  Stack,
} from '@carbon/react';
import Head from 'next/head';
import styles from './scss/App.module.css';

export default function App() {
  const numberInputProps = {
    className: 'some-class',
    id: 'number-input-1',
    label: 'Number Input',
    min: 0,
    max: 100,
    value: 50,
    step: 10,
  };

  const toggleProps = {
    className: 'some-class',
  };

  const radioProps = {
    className: 'some-class',
  };

  const searchProps = {
    className: 'some-class',
  };

  const selectProps = {
    className: 'some-class',
  };

  const TextInputProps = {
    className: 'some-class',
    id: 'test2',
    labelText: 'Text Input label',
    placeholder: 'Placeholder text',
  };

  const textareaProps = {
    labelText: 'Text Area label',
    className: 'some-class',
    placeholder: 'Placeholder text',
    id: 'test5',
    cols: 50,
    rows: 4,
  };

  const buttonEvents = {
    className: 'some-class',
  };

  return (
    <div className={styles.container}>
      <Head>
        <title>Create Next App</title>
        <meta name="description" content="Generated by create next app" />
        <link rel="icon" href="/favicon.ico" />
      </Head>

      <Header aria-label="IBM Platform Name">
        <SkipToContent />
        <HeaderName href="#" prefix="IBM">
          [Platform]
        </HeaderName>
      </Header>
      <Content>
        <Stack gap={6}>
          <NumberInput {...numberInputProps} />
          <Toggle {...toggleProps} id="toggle-1" />
          <RadioButtonGroup
            onChange={() => {}}
            name="radio-button-group"
            defaultSelected="default-selected">
            <RadioButton
              value="standard"
              id="radio-1"
              labelText="Standard Radio Button"
              {...radioProps}
            />
            <RadioButton
              value="default-selected"
              labelText="Default Selected Radio Button"
              id="radio-2"
              {...radioProps}
            />
            <RadioButton
              value="blue"
              labelText="Standard Radio Button"
              id="radio-3"
              {...radioProps}
            />
            <RadioButton
              value="disabled"
              labelText="Disabled Radio Button"
              id="radio-4"
              disabled
              {...radioProps}
            />
          </RadioButtonGroup>
          <StructuredListWrapper>
            <StructuredListHead>
              <StructuredListRow head>
                <StructuredListCell head>ColumnA</StructuredListCell>
                <StructuredListCell head>ColumnB</StructuredListCell>
                <StructuredListCell head>ColumnC</StructuredListCell>
              </StructuredListRow>
            </StructuredListHead>
            <StructuredListBody>
              <StructuredListRow>
                <StructuredListCell noWrap>Row 1</StructuredListCell>
                <StructuredListCell>Row 1</StructuredListCell>
                <StructuredListCell>
                  Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc
                  dui magna, finibus id tortor sed, aliquet bibendum augue.
                  Aenean posuere sem vel euismod dignissim. Nulla ut cursus
                  dolor. Pellentesque vulputate nisl a porttitor interdum.
                </StructuredListCell>
              </StructuredListRow>
              <StructuredListRow>
                <StructuredListCell noWrap>Row 2</StructuredListCell>
                <StructuredListCell>Row 2</StructuredListCell>
                <StructuredListCell>
                  Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc
                  dui magna, finibus id tortor sed, aliquet bibendum augue.
                  Aenean posuere sem vel euismod dignissim. Nulla ut cursus
                  dolor. Pellentesque vulputate nisl a porttitor interdum.
                </StructuredListCell>
              </StructuredListRow>
            </StructuredListBody>
          </StructuredListWrapper>
          <Search
            {...searchProps}
            id="search-1"
            labelText="Search"
            placeholder="Search"
          />
          <Select
            {...selectProps}
            id="select-1"
            defaultValue="placeholder-item">
            <SelectItem
              disabled
              hidden
              value="placeholder-item"
              text="Choose an option"
            />
            <SelectItem value="option-1" text="Option 1" />
            <SelectItem value="option-2" text="Option 2" />
            <SelectItem value="option-3" text="Option 3" />
          </Select>
          <TextInput {...TextInputProps} />
          <TextArea {...textareaProps} />
          <div className={styles.someClass}>
            <Button type="submit" {...buttonEvents}>
              Submit
            </Button>
          </div>
        </Stack>
      </Content>
    </div>
  );
}